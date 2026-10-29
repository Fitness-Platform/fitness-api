package com.fitnessplatform.exercise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExerciseRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        String instructions,

        @Size(max = 120)
        String equipment,

        @Size(max = 2048)
        String videoUrl,

        java.util.UUID videoMediaId
) {
    @jakarta.validation.constraints.AssertTrue(message = "Choose either an external URL or uploaded media")
    public boolean isMediaSourceValid() {
        return videoMediaId == null || videoUrl == null || videoUrl.isBlank();
    }

    public ExerciseRequest(String name, String instructions, String equipment, String videoUrl) {
        this(name, instructions, equipment, videoUrl, null);
    }
}
