package com.fitnessplatform.access;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.user.User;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ProgramAccessRequest(
        @NotNull
        UUID userId,

        @NotNull
        UUID programId,

        @NotNull
        Instant startsAt,

        Instant expiresAt
) {
}
