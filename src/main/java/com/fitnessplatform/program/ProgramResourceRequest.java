package com.fitnessplatform.program;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProgramResourceRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        String description,

        @Size(max = 2048)
        String url,

        @NotNull
        @Positive
        Integer position,

        java.util.UUID mediaId
) {
    @jakarta.validation.constraints.AssertTrue(message = "Choose exactly one external URL or uploaded media")
    public boolean isMediaSourceValid() {
        return (url != null && !url.isBlank()) != (mediaId != null);
    }

    public ProgramResourceRequest(String title, String description, String url, Integer position) {
        this(title, description, url, position, null);
    }
}
