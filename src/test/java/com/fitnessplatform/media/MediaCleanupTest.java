package com.fitnessplatform.media;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaCleanupTest {
    private final Instant now = Instant.parse("2026-10-29T12:00:00Z");
    private final MediaUploadRepository repository = mock(MediaUploadRepository.class);
    private final MediaStorage storage = mock(MediaStorage.class);
    private final MediaCleanup cleanup = new MediaCleanup(repository, storage, Clock.fixed(now, ZoneOffset.UTC));
    MediaUpload upload(Instant created, Instant expires) {
        var upload = new MediaUpload(UUID.randomUUID(), MediaPurpose.EXERCISE_VIDEO, "x.mp4", "video/mp4", 100, created, expires);
        when(repository.findLockedById(upload.getId())).thenReturn(Optional.of(upload));
        return upload;
    }
    @Test void attachedObjectsAreNeverDeletedByAbandonedUploadSweep() {
        var upload = upload(now.minusSeconds(90000), now.minusSeconds(85000));
        upload.attach(); cleanup.clean(upload.getId());
        verifyNoInteractions(storage);
        verify(repository, never()).delete(any());
    }
    @Test void expiredAbandonedUploadDeletesBothKeysAndRow() {
        var upload = upload(now.minusSeconds(90000), now.minusSeconds(85000));
        cleanup.clean(upload.getId());
        verify(storage).delete(upload.getStorageKey());
        verify(storage).delete(upload.getStagingKey());
        verify(repository).delete(upload);
    }
    @Test void retiredFinalIsDeletedButStagingRemainsTrackedUntilPutExpiry() {
        var upload = upload(now, now.plusSeconds(300));
        upload.retire(); cleanup.clean(upload.getId());
        verify(storage).delete(upload.getStorageKey());
        verify(storage, never()).delete(upload.getStagingKey());
        verify(repository, never()).delete(any());
    }
    @Test void storageFailureRetainsRowForRetry() {
        var upload = upload(now.minusSeconds(90000), now.minusSeconds(85000));
        upload.retire();
        doThrow(new MediaException(MediaException.Reason.UNAVAILABLE)).when(storage).delete(upload.getStorageKey());
        assertThrows(MediaException.class, () -> cleanup.clean(upload.getId()));
        verify(repository, never()).delete(any());
    }
    @Test void stagingCleanupCannotDeleteAttachedFinalObject() {
        var upload = upload(now.minusSeconds(90000), now.minusSeconds(85000));
        upload.attach(); cleanup.cleanStaging(upload.getId());
        assertTrue(upload.isStagingCleaned());
        cleanup.cleanStaging(upload.getId());
        verify(storage).delete(upload.getStagingKey());
        verify(storage, never()).delete(upload.getStorageKey());
        verify(repository, never()).delete(any());
    }
    @Test void missingRowCleanupIsIdempotent() {
        cleanup.clean(UUID.randomUUID());
        verifyNoInteractions(storage);
    }
}
