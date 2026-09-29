package com.fitnessplatform.program;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProgramWeekRequest(
        @NotBlank
        @Size(max = 150)
        String title,

        String description,

        @NotNull
        @Positive
        Integer position
) {
}
