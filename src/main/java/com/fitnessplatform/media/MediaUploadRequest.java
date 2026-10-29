package com.fitnessplatform.media;

import jakarta.validation.constraints.*;

public record MediaUploadRequest(@NotNull MediaPurpose purpose,
                                 @NotBlank @Size(max = 255) @Pattern(regexp = "[^\\p{Cntrl}]+") String originalFilename,
                                 @NotBlank @Size(max = 100) String contentType,
                                 @NotNull @Positive Long sizeBytes) {}
