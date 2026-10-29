package com.fitnessplatform.media;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MediaUploadResponse(UUID uploadId, String uploadUrl, Map<String, String> requiredHeaders,
                                  Instant expiresAt) {
    @Override public String toString() { return "MediaUploadResponse[uploadId=" + uploadId + ", uploadUrl=redacted]"; }
}
