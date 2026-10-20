package com.fitnessplatform.program;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MemberWorkoutSetRequest(

        @NotNull
        @Min(0)
        Integer reps,

        @NotNull
        @DecimalMin(value = "0.0")
        @Digits(integer = 6, fraction = 2)
        BigDecimal weightLb

) {
}