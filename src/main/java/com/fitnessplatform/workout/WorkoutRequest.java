package com.fitnessplatform.workout;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkoutRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        String description
) {
}
