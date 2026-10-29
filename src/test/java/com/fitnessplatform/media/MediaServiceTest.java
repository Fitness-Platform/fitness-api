package com.fitnessplatform.media;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {
    @Mock MediaUploadRepository repository;
    @Mock MediaStorage storage;
    @Mock ApplicationEventPublisher events;
    private final UUID admin = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-29T12:00:00Z");
    private MediaService service;
    private MediaUpload upload;

    @BeforeEach void setup() {
        service = new MediaService(repository, storage, new R2Properties(null, null, null, null, null, null),
                Clock.fixed(now, ZoneOffset.UTC), events);
        upload = new MediaUpload(admin, MediaPurpose.EXERCISE_VIDEO, "../private.mp4", "video/mp4", 100, now, now.plusSeconds(300));
    }
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }
    void lock() { when(repository.findLockedById(upload.getId())).thenReturn(Optional.of(upload)); }
    MediaStorage.ObjectMetadata metadata() {
        return new MediaStorage.ObjectMetadata(100, "video/mp4", upload.getId().toString(), "etag");
    }
    void identity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
    }
    void assertReason(MediaException.Reason reason, org.junit.jupiter.api.function.Executable action) {
        assertEquals(reason, assertThrows(MediaException.class, action).reason());
    }

    @Test void allowsAllTypesAndExactBoundaries() {
        for (String type : List.of("video/mp4", "video/webm")) {
            MediaPurpose.EXERCISE_VIDEO.validate(type, 1);
            MediaPurpose.EXERCISE_VIDEO.validate(type, 104857600);
        }
        for (String type : List.of("application/pdf", "image/jpeg", "image/png", "image/webp")) {
            MediaPurpose.PROGRAM_RESOURCE.validate(type, 20971520);
        }
    }
    @Test void rejectsUnsupportedAndInvalidSizes() {
        assertReason(MediaException.Reason.UNSUPPORTED_TYPE, () -> MediaPurpose.EXERCISE_VIDEO.validate("image/png", 1));
        assertReason(MediaException.Reason.UNSUPPORTED_TYPE, () -> MediaPurpose.PROGRAM_RESOURCE.validate("image/svg+xml", 1));
        assertReason(MediaException.Reason.INVALID_SIZE, () -> MediaPurpose.EXERCISE_VIDEO.validate("video/mp4", 0));
        assertReason(MediaException.Reason.INVALID_SIZE, () -> MediaPurpose.PROGRAM_RESOURCE.validate("application/pdf", -1));
        assertReason(MediaException.Reason.TOO_LARGE, () -> MediaPurpose.EXERCISE_VIDEO.validate("video/mp4", 104857601));
        assertReason(MediaException.Reason.TOO_LARGE, () -> MediaPurpose.PROGRAM_RESOURCE.validate("application/pdf", 20971521));
        verifyNoInteractions(storage);
    }
    @Test void authorizationGeneratesUniqueKeysAndShortGrant() {
        when(storage.presignPut(anyString(), anyString(), anyString(), eq(Duration.ofMinutes(5))))
                .thenReturn(new MediaStorage.UploadUrl("https://example.test/signed", Map.of("Content-Type", "video/mp4")));
        var request = new MediaUploadRequest(MediaPurpose.EXERCISE_VIDEO, "original.mp4", "video/mp4", 100L);
        var first = service.authorize(admin, request);
        var second = service.authorize(admin, request);
        var captor = ArgumentCaptor.forClass(MediaUpload.class);
        verify(repository, times(2)).save(captor.capture());
        var a = captor.getAllValues().get(0);
        var b = captor.getAllValues().get(1);
        assertNotEquals(first.uploadId(), second.uploadId());
        assertNotEquals(a.getStorageKey(), b.getStorageKey());
        assertTrue(a.getStagingKey().startsWith("exercise-videos/pending/"));
        assertFalse(a.getStorageKey().contains("original.mp4"));
        assertNotEquals(a.getStagingKey(), a.getStorageKey());
        assertEquals(now.plusSeconds(300), first.expiresAt());
    }
    @Test void unsupportedAuthorizationNeverCallsStorage() {
        assertReason(MediaException.Reason.UNSUPPORTED_TYPE, () -> service.authorize(admin,
                new MediaUploadRequest(MediaPurpose.EXERCISE_VIDEO, "x", "text/html", 10L)));
        verifyNoInteractions(storage, repository);
    }
    @Test void invalidReferenceIsSanitized() {
        assertReason(MediaException.Reason.INVALID_REFERENCE, () -> service.complete(admin, UUID.randomUUID()));
        verifyNoInteractions(storage);
    }
    @Test void cannotCompleteAnotherAdminsUpload() {
        lock();
        assertReason(MediaException.Reason.INVALID_REFERENCE, () -> service.complete(UUID.randomUUID(), upload.getId()));
        verifyNoInteractions(storage);
    }
    @Test void expiredUploadCannotComplete() {
        upload = new MediaUpload(admin, MediaPurpose.EXERCISE_VIDEO, "x", "video/mp4", 100, now.minusSeconds(301), now);
        lock();
        assertReason(MediaException.Reason.EXPIRED, () -> service.complete(admin, upload.getId()));
        verifyNoInteractions(storage);
    }
    @Test void missingObjectCannotComplete() {
        lock();
        when(storage.inspect(upload.getStagingKey())).thenReturn(Optional.empty());
        assertReason(MediaException.Reason.MISSING, () -> service.complete(admin, upload.getId()));
        assertEquals(MediaUpload.State.PENDING, upload.getState());
        verify(storage, never()).copyVerified(any(), any(), any());
    }
    @Test void mismatchedSizeTypeOrReferenceDeletesStagingAndRejects() {
        lock();
        for (var bad : List.of(new MediaStorage.ObjectMetadata(101, "video/mp4", upload.getId().toString(), "e"),
                new MediaStorage.ObjectMetadata(100, "text/html", upload.getId().toString(), "e"),
                new MediaStorage.ObjectMetadata(100, "video/mp4", "another-upload", "e"))) {
            when(storage.inspect(upload.getStagingKey())).thenReturn(Optional.of(bad));
            assertReason(MediaException.Reason.VERIFICATION_FAILED, () -> service.complete(admin, upload.getId()));
        }
        verify(storage, times(3)).delete(upload.getStagingKey());
        verify(storage, never()).copyVerified(any(), any(), any());
    }
    @Test void verifiedCopyMustAlsoMatch() {
        lock();
        when(storage.inspect(upload.getStagingKey())).thenReturn(Optional.of(metadata()));
        when(storage.inspect(upload.getStorageKey())).thenReturn(Optional.of(
                new MediaStorage.ObjectMetadata(101, "video/mp4", upload.getId().toString(), "e")));
        assertReason(MediaException.Reason.VERIFICATION_FAILED, () -> service.complete(admin, upload.getId()));
        verify(storage).delete(upload.getStorageKey());
        assertEquals(MediaUpload.State.PENDING, upload.getState());
    }
    @Test void completionCopiesInspectedEtagToUnpresignedKeyThenVerifiesAndIsIdempotent() {
        lock();
        when(storage.inspect(upload.getStagingKey())).thenReturn(Optional.of(metadata()));
        when(storage.inspect(upload.getStorageKey())).thenReturn(Optional.of(metadata()));
        var response = service.complete(admin, upload.getId());
        var order = inOrder(storage);
        order.verify(storage).inspect(upload.getStagingKey());
        order.verify(storage).copyVerified(upload.getStagingKey(), upload.getStorageKey(), "etag");
        order.verify(storage).inspect(upload.getStorageKey());
        assertEquals(MediaUpload.State.VERIFIED, upload.getState());
        assertEquals("../private.mp4", response.originalFilename());
        service.complete(admin, upload.getId());
        verify(storage, times(1)).copyVerified(any(), any(), any());
    }
    @Test void attachingRequiresVerifiedCorrectPurposeAndOwner() {
        lock(); identity();
        assertReason(MediaException.Reason.NOT_COMPLETED,
                () -> service.attach(upload.getId(), MediaPurpose.EXERCISE_VIDEO, null));
        upload.verified();
        assertReason(MediaException.Reason.INVALID_REFERENCE,
                () -> service.attach(upload.getId(), MediaPurpose.PROGRAM_RESOURCE, null));
        SecurityContextHolder.clearContext();
        assertReason(MediaException.Reason.INVALID_REFERENCE,
                () -> service.attach(upload.getId(), MediaPurpose.EXERCISE_VIDEO, null));
    }
    @Test void verifiedMediaCannotHaveTwoOwnersButCurrentReferenceCanBeRetained() {
        lock(); identity(); upload.verified();
        when(storage.inspect(upload.getStorageKey())).thenReturn(Optional.of(metadata()));
        assertSame(upload, service.attach(upload.getId(), MediaPurpose.EXERCISE_VIDEO, null));
        assertSame(upload, service.attach(upload.getId(), MediaPurpose.EXERCISE_VIDEO, upload));
        assertReason(MediaException.Reason.INVALID_REFERENCE,
                () -> service.attach(upload.getId(), MediaPurpose.EXERCISE_VIDEO, null));
    }
    @Test void retiredMediaCannotBeReattached() {
        lock(); identity(); upload.retire();
        assertReason(MediaException.Reason.INVALID_REFERENCE,
                () -> service.attach(upload.getId(), MediaPurpose.EXERCISE_VIDEO, null));
    }
    @Test void missingVerifiedObjectCannotAttach() {
        lock(); identity(); upload.verified();
        when(storage.inspect(upload.getStorageKey())).thenReturn(Optional.empty());
        assertReason(MediaException.Reason.MISSING,
                () -> service.attach(upload.getId(), MediaPurpose.EXERCISE_VIDEO, null));
    }
    @Test void retiringOnlyPublishesAfterCommitWorkNeverDeletesInsideDomainTransaction() {
        lock(); upload.attach();
        service.retireReplaced(upload, null);
        assertEquals(MediaUpload.State.RETIRED, upload.getState());
        verify(events).publishEvent(new MediaService.Retired(upload.getId()));
        verifyNoInteractions(storage);
    }
    @Test void sameReferenceAndExternalRemovalDoNotDeleteAnything() {
        service.retireReplaced(upload, upload);
        service.retireReplaced(null, null);
        verifyNoInteractions(storage, events, repository);
    }
    @Test void memberUrlIsShortLivedAndExternalUrlUnchanged() {
        upload.attach();
        when(storage.presignGet(upload.getStorageKey(), Duration.ofMinutes(10))).thenReturn("https://private.test/signed");
        assertEquals("https://private.test/signed", service.resolveUrl(null, upload));
        assertEquals("https://external.test/file", service.resolveUrl("https://external.test/file", null));
        assertNull(service.resolveUrl(null, null));
    }
    @Test void conflictingSourcesAndMissingRequiredSourceRejected() {
        assertReason(MediaException.Reason.SOURCE_CONFLICT, () -> MediaService.validateSources("https://x", upload.getId(), false));
        assertReason(MediaException.Reason.SOURCE_REQUIRED, () -> MediaService.validateSources(" ", null, true));
        MediaService.validateSources(null, null, false);
    }
    @Test void ttlConfigurationIsBoundedAndSecretsAreRedacted() {
        var valid = new R2Properties("a".repeat(32), "key", "secret", "bucket", null, null);
        assertTrue(valid.configured()); assertTrue(valid.isTtlValid());
        assertFalse(valid.toString().contains("secret"));
        assertFalse(new R2Properties(null, null, null, null, Duration.ZERO, null).isTtlValid());
        assertFalse(new R2Properties(null, null, null, null, null, Duration.ofMinutes(16)).isTtlValid());
    }
}
