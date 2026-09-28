package com.fitnessplatform.workout;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkoutExerciseCreateRequest(

        @NotNull
        UUID exerciseId,

        @NotNull
        @Positive
        Integer sets,

        @NotBlank
        @Size(max = 50)
        String reps,

        @DecimalMin(value = "0.0")
        @Digits(integer = 6, fraction = 2)
        BigDecimal suggestedWeightLb,

        @PositiveOrZero
        Integer restSeconds,

        String notes,

        @NotNull
        @Positive
        Integer position
) {
}