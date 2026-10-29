package com.fitnessplatform.media;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class R2MediaStorageTest {
    @Test void presignsPrivateEndpointWithBoundedTtlAndRequiredUploadHeadersWithoutNetwork() {
        var properties = new R2Properties("a".repeat(32), "test-access-key", "test-secret", "private-bucket", null, null);
        var storage = new R2MediaStorage(properties);
        try {
            var put = storage.presignPut("exercise-videos/pending/random", "video/mp4", "upload-ref", Duration.ofMinutes(5));
            assertTrue(put.url().startsWith("https://" + "a".repeat(32) + ".r2.cloudflarestorage.com/private-bucket/"));
            assertTrue(put.url().contains("X-Amz-Expires=300"));
            assertTrue(put.url().contains("content-type"));
            assertTrue(put.url().contains("x-amz-meta-upload-id"));
            assertEquals("video/mp4", put.requiredHeaders().get("Content-Type"));
            assertEquals("upload-ref", put.requiredHeaders().get("x-amz-meta-upload-id"));
            assertFalse(put.url().contains("test-secret"));
            assertFalse(put.toString().contains(put.url()));
            assertFalse(new MediaUploadResponse(java.util.UUID.randomUUID(), put.url(), put.requiredHeaders(),
                    java.time.Instant.now()).toString().contains(put.url()));
            String get = storage.presignGet("exercise-videos/verified/other", Duration.ofMinutes(10));
            assertTrue(get.contains("X-Amz-Expires=600"));
            assertTrue(get.contains("response-cache-control="));
        } finally { storage.close(); }
    }
    @Test void missingConfigurationFailsSafelyOnlyForMediaOperations() {
        var storage = new R2MediaStorage(new R2Properties(null, null, null, null, null, null));
        try {
            var exception = assertThrows(MediaException.class, () -> storage.inspect("key"));
            assertEquals(MediaException.Reason.UNAVAILABLE, exception.reason());
            assertFalse(exception.getMessage().contains("key"));
        } finally { storage.close(); }
    }
}
