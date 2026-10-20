package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccessWorkoutSet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MemberWorkoutSetResponse(
        UUID id,
        UUID workoutExerciseId,
        Integer setNumber,
        Integer reps,
        BigDecimal weightLb,
        Instant completedAt
) {

    public static MemberWorkoutSetResponse from(
            ProgramAccessWorkoutSet set
    ) {
        return new MemberWorkoutSetResponse(
                set.getId(),
                set.getWorkoutExercise().getId(),
                set.getSetNumber(),
                set.getReps(),
                set.getWeightLb(),
                set.getCompletedAt()
        );
    }
}