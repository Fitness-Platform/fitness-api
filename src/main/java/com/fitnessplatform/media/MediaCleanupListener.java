package com.fitnessplatform.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Clock;
import java.time.Duration;

@Component
public class MediaCleanupListener {
    private static final Logger log = LoggerFactory.getLogger(MediaCleanupListener.class);
    private final MediaCleanup cleanup;
    private final MediaUploadRepository repository;
    private final Clock clock;
    public MediaCleanupListener(MediaCleanup cleanup, MediaUploadRepository repository, Clock clock) {
        this.cleanup = cleanup;
        this.repository = repository;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(MediaService.Retired event) {
        try { cleanup.clean(event.uploadId()); }
        catch (RuntimeException ignored) {
            // Never log provider exceptions, keys or signed URLs. The RETIRED row remains retryable.
            log.warn("Media cleanup deferred; scheduled retry will attempt again.");
        }
    }

    @Scheduled(fixedDelayString = "${media.cleanup-delay-ms:3600000}", initialDelayString = "${media.cleanup-delay-ms:3600000}")
    public void sweep() {
        for (var id : repository.findCleanupCandidates(clock.instant().minus(Duration.ofHours(24)))) {
            try { cleanup.clean(id); }
            catch (RuntimeException ignored) { log.warn("Media cleanup deferred."); }
        }
        for (var id : repository.findStagingCleanupCandidates(clock.instant().minus(Duration.ofHours(1)))) {
            try { cleanup.cleanStaging(id); }
            catch (RuntimeException ignored) { log.warn("Media staging cleanup deferred."); }
        }
    }
}
