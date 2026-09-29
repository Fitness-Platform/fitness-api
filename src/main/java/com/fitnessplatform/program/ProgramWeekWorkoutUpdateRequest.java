package com.fitnessplatform.program;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProgramWeekWorkoutUpdateRequest(
        @NotNull
        @Positive
        Integer position
) {
}
