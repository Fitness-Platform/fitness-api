package com.fitnessplatform.media;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** All keys are backend-generated; implementations must sanitize provider errors. */
public interface MediaStorage {
    record ObjectMetadata(long sizeBytes, String contentType, String uploadId, String etag) {}
    record UploadUrl(String url, Map<String, String> requiredHeaders) {
        @Override public String toString() { return "UploadUrl[redacted]"; }
    }

    UploadUrl presignPut(String stagingKey, String contentType, String uploadId, Duration ttl);
    String presignGet(String key, Duration ttl);
    Optional<ObjectMetadata> inspect(String key);
    void copyVerified(String stagingKey, String destinationKey, String expectedEtag);
    void delete(String key);
}
