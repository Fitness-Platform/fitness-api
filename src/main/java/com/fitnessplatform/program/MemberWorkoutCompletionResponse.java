package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccessWorkout;

import java.time.Instant;
import java.util.UUID;

public record MemberWorkoutCompletionResponse(
        UUID programAccessWorkoutId,
        UUID programWeekWorkoutId,
        Instant startedAt,
        Instant completedAt
) {

    public static MemberWorkoutCompletionResponse from(
            ProgramAccessWorkout accessWorkout
    ) {
        return new MemberWorkoutCompletionResponse(
                accessWorkout.getId(),
                accessWorkout
                        .getProgramWeekWorkout()
                        .getId(),
                accessWorkout.getStartedAt(),
                accessWorkout.getCompletedAt()
        );
    }
}