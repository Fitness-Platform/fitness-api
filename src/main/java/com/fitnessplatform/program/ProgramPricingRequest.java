package com.fitnessplatform.program;

import jakarta.validation.constraints.Positive;

public record ProgramPricingRequest(

        @Positive
        Long priceCents
) {
}
