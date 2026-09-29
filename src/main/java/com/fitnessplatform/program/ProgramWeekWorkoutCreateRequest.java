package com.fitnessplatform.program;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ProgramWeekWorkoutCreateRequest(
        @NotNull
        UUID workoutId,

        @NotNull
        @Positive
        Integer position
) {
}
