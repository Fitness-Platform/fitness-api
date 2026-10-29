package com.fitnessplatform.media;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
public class MediaService {
    public record Retired(UUID uploadId) {}
    private final MediaUploadRepository repository;
    private final MediaStorage storage;
    private final R2Properties properties;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public MediaService(MediaUploadRepository repository, MediaStorage storage, R2Properties properties,
                        Clock clock, ApplicationEventPublisher events) {
        this.repository = repository;
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public MediaUploadResponse authorize(UUID adminId, MediaUploadRequest request) {
        request.purpose().validate(request.contentType(), request.sizeBytes());
        MediaUpload upload = new MediaUpload(adminId, request.purpose(), request.originalFilename().strip(),
                request.contentType(), request.sizeBytes(), clock.instant(), clock.instant().plus(properties.uploadTtl()));
        var signed = storage.presignPut(upload.getStagingKey(), upload.getContentType(),
                upload.getId().toString(), properties.uploadTtl());
        repository.save(upload);
        // Neither final storage keys nor credentials are part of the client attachment contract.
        return new MediaUploadResponse(upload.getId(), signed.url(), signed.requiredHeaders(), upload.getUploadExpiresAt());
    }

    @Transactional
    public MediaMetadataResponse complete(UUID adminId, UUID uploadId) {
        MediaUpload upload = requireLocked(uploadId);
        requireOwner(upload, adminId);
        if (upload.getState() == MediaUpload.State.VERIFIED || upload.getState() == MediaUpload.State.ATTACHED) {
            return MediaMetadataResponse.from(upload);
        }
        if (upload.getState() != MediaUpload.State.PENDING) invalidReference();
        if (!clock.instant().isBefore(upload.getUploadExpiresAt())) {
            throw new MediaException(MediaException.Reason.EXPIRED);
        }
        var source = storage.inspect(upload.getStagingKey())
                .orElseThrow(() -> new MediaException(MediaException.Reason.MISSING));
        if (!matches(upload, source) || source.etag() == null || source.etag().isBlank()) {
            deleteInvalid(upload.getStagingKey());
            throw new MediaException(MediaException.Reason.VERIFICATION_FAILED);
        }
        // The PUT grant can still be reused until expiry. Freeze only the inspected version
        // under a separate key that was never presigned for PUT. ETag prevents a HEAD/COPY race.
        storage.copyVerified(upload.getStagingKey(), upload.getStorageKey(), source.etag());
        var frozen = storage.inspect(upload.getStorageKey())
                .orElseThrow(() -> new MediaException(MediaException.Reason.MISSING));
        if (!matches(upload, frozen)) {
            deleteInvalid(upload.getStorageKey());
            throw new MediaException(MediaException.Reason.VERIFICATION_FAILED);
        }
        upload.verified();
        // Staging cleanup waits until the PUT grant has expired (scheduled cleanup).
        return MediaMetadataResponse.from(upload);
    }

    /** Called inside the owning domain transaction; a verified reference can have only one owner. */
    @Transactional
    public MediaUpload attach(UUID uploadId, MediaPurpose purpose, MediaUpload current) {
        if (uploadId == null) return null;
        MediaUpload upload = requireLocked(uploadId);
        if (upload.getPurpose() != purpose) invalidReference();
        if (current != null && current.getId().equals(uploadId) && upload.getState() == MediaUpload.State.ATTACHED) {
            return upload;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID adminId)) invalidReference();
        else requireOwner(upload, adminId);
        if (upload.getState() != MediaUpload.State.VERIFIED) {
            throw new MediaException(upload.getState() == MediaUpload.State.PENDING
                    ? MediaException.Reason.NOT_COMPLETED : MediaException.Reason.INVALID_REFERENCE);
        }
        if (upload.getCreatedAt().plus(Duration.ofHours(24)).isBefore(clock.instant())) {
            throw new MediaException(MediaException.Reason.EXPIRED);
        }
        // Fail closed if the private object disappeared after completion.
        var metadata = storage.inspect(upload.getStorageKey())
                .orElseThrow(() -> new MediaException(MediaException.Reason.MISSING));
        if (!matches(upload, metadata)) throw new MediaException(MediaException.Reason.VERIFICATION_FAILED);
        upload.attach();
        return upload;
    }

    @Transactional
    public void retireReplaced(MediaUpload old, MediaUpload replacement) {
        if (old == null || (replacement != null && old.getId().equals(replacement.getId()))) return;
        MediaUpload locked = requireLocked(old.getId());
        locked.retire();
        // State and domain reference change commit atomically. No storage deletion before commit.
        events.publishEvent(new Retired(locked.getId()));
    }

    /** Invoke only after the existing Member access and unlock checks succeed. */
    public String resolveUrl(String externalUrl, MediaUpload upload) {
        if (upload == null) return externalUrl;
        if (upload.getState() != MediaUpload.State.ATTACHED) invalidReference();
        return storage.presignGet(upload.getStorageKey(), properties.getTtl());
    }

    public static void validateSources(String url, UUID uploadId, boolean required) {
        boolean external = url != null && !url.isBlank();
        if (external && uploadId != null) throw new MediaException(MediaException.Reason.SOURCE_CONFLICT);
        if (required && !external && uploadId == null) throw new MediaException(MediaException.Reason.SOURCE_REQUIRED);
    }

    private MediaUpload requireLocked(UUID id) {
        return repository.findLockedById(id).orElseThrow(() -> new MediaException(MediaException.Reason.INVALID_REFERENCE));
    }
    private void requireOwner(MediaUpload upload, UUID adminId) {
        if (!upload.getUploaderId().equals(adminId)) invalidReference();
    }
    private void invalidReference() { throw new MediaException(MediaException.Reason.INVALID_REFERENCE); }
    private boolean matches(MediaUpload upload, MediaStorage.ObjectMetadata metadata) {
        return metadata.sizeBytes() == upload.getSizeBytes()
                && Objects.equals(metadata.contentType(), upload.getContentType())
                && Objects.equals(metadata.uploadId(), upload.getId().toString());
    }
    private void deleteInvalid(String key) {
        try { storage.delete(key); } catch (MediaException ignored) { /* Scheduled retry cleans abandoned uploads. */ }
    }
}
