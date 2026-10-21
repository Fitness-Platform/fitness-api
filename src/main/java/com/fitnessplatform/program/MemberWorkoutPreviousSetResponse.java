package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccessWorkoutSet;

import java.math.BigDecimal;

public record MemberWorkoutPreviousSetResponse(
        Integer setNumber,
        Integer reps,
        BigDecimal weightLb
) {

    public static MemberWorkoutPreviousSetResponse from(
            ProgramAccessWorkoutSet set
    ) {
        return new MemberWorkoutPreviousSetResponse(
                set.getSetNumber(),
                set.getReps(),
                set.getWeightLb()
        );
    }
}