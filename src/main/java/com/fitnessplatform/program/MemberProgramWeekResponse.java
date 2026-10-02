package com.fitnessplatform.program;

import java.time.Instant;
import java.util.UUID;

public record MemberProgramWeekResponse(
        UUID weekId,
        String title,
        String description,
        Integer position,
        boolean unlocked,
        Instant unlocksAt
) {

    public static MemberProgramWeekResponse from(
            ProgramWeek week,
            boolean unlocked,
            Instant unlocksAt
    ) {
        return new MemberProgramWeekResponse(
                week.getId(),
                week.getTitle(),
                week.getDescription(),
                week.getPosition(),
                unlocked,
                unlocksAt
        );
    }
}
