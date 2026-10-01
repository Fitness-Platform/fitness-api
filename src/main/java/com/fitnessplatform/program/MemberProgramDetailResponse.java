package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccess;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemberProgramDetailResponse(
        UUID programId,
        String name,
        String description,
        Instant accessStartsAt,
        Instant accessExpiresAt,
        List<MemberProgramResourceResponse> resources,
        List<MemberProgramWeekResponse> weeks
) {
    public static MemberProgramDetailResponse from(
            ProgramAccess access,
            List<ProgramResource> resources,
            List<ProgramWeek> weeks
    ) {
        Program program =
                access.getProgram();

        return new MemberProgramDetailResponse(
                program.getId(),
                program.getName(),
                program.getDescription(),
                access.getStartsAt(),
                access.getExpiresAt(),
                resources.stream()
                        .map(
                                MemberProgramResourceResponse::from
                        )
                        .toList(),
                weeks.stream()
                        .map(
                                MemberProgramWeekResponse::from
                        )
                        .toList()
        );
    }
}
