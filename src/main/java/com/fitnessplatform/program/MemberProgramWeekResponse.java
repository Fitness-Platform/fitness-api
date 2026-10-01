package com.fitnessplatform.program;

import java.util.UUID;

public record MemberProgramWeekResponse(
        UUID weekId,
        String title,
        String description,
        Integer position
) {

    public static MemberProgramWeekResponse from(
            ProgramWeek week
    ) {
        return new MemberProgramWeekResponse(
                week.getId(),
                week.getTitle(),
                week.getDescription(),
                week.getPosition()
        );
    }
}
