package com.fitnessplatform.workout;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;


public record WorkoutExerciseUpdateRequest(

        @NotNull
        @Positive
        Integer sets,

        @NotBlank
        @Size(max = 50)
        String reps,

        @DecimalMin(value="0.0")
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
