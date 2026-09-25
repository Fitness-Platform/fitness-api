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
        String videoUrl
) {
}
