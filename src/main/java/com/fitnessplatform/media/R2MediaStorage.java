package com.fitnessplatform.media;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;
import jakarta.annotation.PreDestroy;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class R2MediaStorage implements MediaStorage {
    private final R2Properties properties;
    private final S3Client client;
    private final S3Presigner presigner;

    public R2MediaStorage(R2Properties properties) {
        this.properties = properties;
        if (properties.configured()) {
            URI endpoint = URI.create("https://" + properties.accountId() + ".r2.cloudflarestorage.com");
            var credentials = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKeyId(), properties.secretAccessKey()));
            client = S3Client.builder().endpointOverride(endpoint).region(Region.of("auto"))
                    .credentialsProvider(credentials).forcePathStyle(true)
                    .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                    .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                    .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(30))
                            .apiCallAttemptTimeout(Duration.ofSeconds(10))).build();
            presigner = S3Presigner.builder().endpointOverride(endpoint).region(Region.of("auto"))
                    .credentialsProvider(credentials)
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
        } else {
            // Existing external URL deployments and tests do not require R2 credentials.
            client = null;
            presigner = null;
        }
    }

    @Override
    public UploadUrl presignPut(String key, String contentType, String uploadId, Duration ttl) {
        return safely(() -> {
            var request = PutObjectRequest.builder().bucket(properties.bucketName()).key(key)
                    .contentType(contentType).metadata(Map.of("upload-id", uploadId)).build();
            var signed = presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(ttl).putObjectRequest(request).build());
            return new UploadUrl(signed.url().toString(), Map.of(
                    "Content-Type", contentType, "x-amz-meta-upload-id", uploadId));
        });
    }

    @Override
    public String presignGet(String key, Duration ttl) {
        return safely(() -> presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl).getObjectRequest(GetObjectRequest.builder()
                        .bucket(properties.bucketName()).key(key)
                        .responseCacheControl("private, no-store").build()).build()).url().toString());
    }

    @Override
    public Optional<ObjectMetadata> inspect(String key) {
        return safely(() -> {
            try {
                var head = client.headObject(HeadObjectRequest.builder().bucket(properties.bucketName()).key(key).build());
                return Optional.of(new ObjectMetadata(head.contentLength(), head.contentType(),
                        head.metadata().get("upload-id"), head.eTag()));
            } catch (S3Exception exception) {
                if (exception.statusCode() == 404) return Optional.empty();
                throw exception;
            }
        });
    }

    @Override
    public void copyVerified(String source, String destination, String etag) {
        safely(() -> {
            try {
                client.copyObject(CopyObjectRequest.builder().bucket(properties.bucketName()).key(destination)
                        .copySource(properties.bucketName() + "/" + source).copySourceIfMatch(etag).build());
            } catch (S3Exception exception) {
                if (exception.statusCode() == 412) throw new MediaException(MediaException.Reason.VERIFICATION_FAILED);
                throw exception;
            }
            return null;
        });
    }

    @Override
    public void delete(String key) {
        safely(() -> {
            client.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucketName()).key(key).build());
            return null;
        });
    }

    private <T> T safely(Supplier<T> action) {
        if (client == null) throw new MediaException(MediaException.Reason.UNAVAILABLE);
        try { return action.get(); }
        catch (MediaException exception) { throw exception; }
        catch (RuntimeException exception) {
            // Deliberately discard provider messages/causes, which can contain signed requests.
            throw new MediaException(MediaException.Reason.UNAVAILABLE);
        }
    }

    @PreDestroy
    void close() {
        if (client != null) client.close();
        if (presigner != null) presigner.close();
    }
}
