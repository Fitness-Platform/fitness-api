package com.fitnessplatform.media;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Service
public class MediaCleanup {
    private final MediaUploadRepository repository;
    private final MediaStorage storage;
    private final Clock clock;
    public MediaCleanup(MediaUploadRepository repository, MediaStorage storage, Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clean(UUID uploadId) {
        var upload = repository.findLockedById(uploadId).orElse(null);
        if (upload == null) return;
        if (upload.getState() != MediaUpload.State.RETIRED &&
                (upload.getState() == MediaUpload.State.ATTACHED ||
                        !upload.getCreatedAt().plus(Duration.ofHours(24)).isBefore(clock.instant()))) return;
        // Delete is idempotent. Keep the durable row for retries if either provider call fails.
        storage.delete(upload.getStorageKey());
        if (upload.getUploadExpiresAt().plus(Duration.ofHours(1)).isBefore(clock.instant())) {
            storage.delete(upload.getStagingKey());
            repository.delete(upload);
        }
        // A retired row stays retryable until its PUT grant/in-flight requests have expired.

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanStaging(UUID uploadId) {
        var upload = repository.findLockedById(uploadId).orElse(null);
        if (upload != null && !upload.isStagingCleaned() && upload.getState() == MediaUpload.State.ATTACHED
                && upload.getUploadExpiresAt().plus(Duration.ofHours(1)).isBefore(clock.instant())) {
            storage.delete(upload.getStagingKey());
            upload.markStagingCleaned();
        }
    }
}
