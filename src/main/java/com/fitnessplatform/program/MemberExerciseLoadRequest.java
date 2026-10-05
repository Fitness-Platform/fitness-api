package com.fitnessplatform.program;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MemberExerciseLoadRequest(

        @NotNull
        @DecimalMin(value = "0.0")
        @Digits(integer = 6, fraction = 2)
        BigDecimal weightLb

) {
}