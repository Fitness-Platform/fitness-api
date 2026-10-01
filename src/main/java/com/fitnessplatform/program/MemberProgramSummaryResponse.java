package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccess;

import java.time.Instant;
import java.util.UUID;

public record MemberProgramSummaryResponse(
        UUID programId,
        String name,
        String description,
        Instant accessStartsAt,
        Instant accessExpiresAt
) {

    public static MemberProgramSummaryResponse  from(
            ProgramAccess access
    ) {
        Program program =
                access.getProgram();

        return new MemberProgramSummaryResponse(
                program.getId(),
                program.getName(),
                program.getDescription(),
                access.getStartsAt(),
                access.getExpiresAt()
        );
    }
}
