package com.fitnessplatform.program;

import java.util.UUID;

public record PublicProgramResponse(
        UUID id,
        String name,
        String description,
        Long priceCents,
        String currency
) {
    public static PublicProgramResponse from(Program program) {
        return new PublicProgramResponse(
                program.getId(),
                program.getName(),
                program.getDescription(),
                program.getPriceCents(),
                program.getCurrency()
        );
    }
}
