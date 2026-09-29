package com.fitnessplatform.program;

import java.time.Instant;
import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String name,
        String description,
        ProgramStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProgramResponse from(
            Program program
    ) {
        return new ProgramResponse(
                program.getId(),
                program.getName(),
                program.getDescription(),
                program.getStatus(),
                program.getCreatedAt(),
                program.getUpdatedAt()
        );
    }
}
