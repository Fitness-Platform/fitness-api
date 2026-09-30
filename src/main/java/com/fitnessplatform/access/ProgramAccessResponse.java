package com.fitnessplatform.access;

import java.time.Instant;
import java.util.UUID;

public record ProgramAccessResponse(
        UUID id,
        UUID userId,
        UUID programId,
        Instant startsAt,
        Instant expiresAt,
        Instant revokedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProgramAccessResponse from(ProgramAccess access) {
        return new ProgramAccessResponse(
                access.getId(),
                access.getUser().getId(),
                access.getProgram().getId(),
                access.getStartsAt(),
                access.getExpiresAt(),
                access.getRevokedAt(),
                access.getCreatedAt(),
                access.getUpdatedAt()
        );
    }
}
