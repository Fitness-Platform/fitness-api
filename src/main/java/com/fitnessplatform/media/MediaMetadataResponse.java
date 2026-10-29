package com.fitnessplatform.media;

import java.util.UUID;

public record MediaMetadataResponse(UUID id, String sourceType, String originalFilename,
                                    String contentType, long sizeBytes) {
    public static MediaMetadataResponse from(MediaUpload upload) {
        return upload == null ? null : new MediaMetadataResponse(upload.getId(), "UPLOADED",
                upload.getOriginalFilename(), upload.getContentType(), upload.getSizeBytes());
    }
}
