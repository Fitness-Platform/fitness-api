package com.fitnessplatform.program;

import java.time.Instant;
import java.util.UUID;

public record ProgramResourceResponse(
        UUID id,
        UUID programId,
        String title,
        String description,
        String url,
        Integer position,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProgramResourceResponse from(
            ProgramResource resource
    ) {
        return new ProgramResourceResponse(
                resource.getId(),
                resource.getProgram().getId(),
                resource.getTitle(),
                resource.getDescription(),
                resource.getUrl(),
                resource.getPosition(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
