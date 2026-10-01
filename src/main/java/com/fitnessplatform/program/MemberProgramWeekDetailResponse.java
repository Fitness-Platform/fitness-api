package com.fitnessplatform.program;

import java.util.List;
import java.util.UUID;

public record MemberProgramWeekDetailResponse(
        UUID weekId,
        String title,
        String description,
        Integer position,
        List<MemberProgramWorkoutSummaryResponse> workouts
) {

    public static MemberProgramWeekDetailResponse from(
            ProgramWeek week,
            List<ProgramWeekWorkout> workouts
    ) {
        return new MemberProgramWeekDetailResponse(
                week.getId(),
                week.getTitle(),
                week.getDescription(),
                week.getPosition(),
                workouts.stream()
                        .map(
                                MemberProgramWorkoutSummaryResponse::from
                        )
                        .toList()
        );
    }
}
