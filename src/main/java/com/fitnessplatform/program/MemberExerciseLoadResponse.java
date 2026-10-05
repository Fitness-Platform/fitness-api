package com.fitnessplatform.program;

import com.fitnessplatform.workout.WorkoutExercise;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberExerciseLoadResponse(
        UUID workoutExerciseId,
        BigDecimal suggestedWeightLb,
        BigDecimal currentWeightLb
) {

    public static MemberExerciseLoadResponse from(
            WorkoutExercise workoutExercise,
            BigDecimal currentWeightLb
    ) {
        return new MemberExerciseLoadResponse(
                workoutExercise.getId(),
                workoutExercise.getSuggestedWeightLb(),
                currentWeightLb
        );
    }
}