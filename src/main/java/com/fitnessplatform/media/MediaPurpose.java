package com.fitnessplatform.media;

import java.util.Set;

public enum MediaPurpose {
    EXERCISE_VIDEO("exercise-videos", 100L * 1024 * 1024, Set.of("video/mp4", "video/webm")),
    PROGRAM_RESOURCE("program-resources", 20L * 1024 * 1024,
            Set.of("application/pdf", "image/jpeg", "image/png", "image/webp"));

    private final String prefix;
    private final long maxBytes;
    private final Set<String> contentTypes;

    MediaPurpose(String prefix, long maxBytes, Set<String> contentTypes) {
        this.prefix = prefix;
        this.maxBytes = maxBytes;
        this.contentTypes = contentTypes;
    }

    public String prefix() { return prefix; }
    public long maxBytes() { return maxBytes; }

    public void validate(String contentType, long sizeBytes) {
        if (!contentTypes.contains(contentType)) {
            throw new MediaException(MediaException.Reason.UNSUPPORTED_TYPE);
        }
        if (sizeBytes <= 0) throw new MediaException(MediaException.Reason.INVALID_SIZE);
        if (sizeBytes > maxBytes) throw new MediaException(MediaException.Reason.TOO_LARGE);
    }
}
