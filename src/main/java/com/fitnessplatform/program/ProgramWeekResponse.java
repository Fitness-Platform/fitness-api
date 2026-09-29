package com.fitnessplatform.program;

import java.time.Instant;
import java.util.UUID;

public record ProgramWeekResponse(
        UUID id,
        UUID programId,
        String title,
        String description,
        Integer position,
        Instant createdAt,
        Instant updatedAt

) {
    public static ProgramWeekResponse from(ProgramWeek programWeek) {
        return new ProgramWeekResponse(
                programWeek.getId(),
                programWeek.getProgram().getId(),
                programWeek.getTitle(),
                programWeek.getDescription(),
                programWeek.getPosition(),
                programWeek.getCreatedAt(),
                programWeek.getUpdatedAt()
        );
    }
}
