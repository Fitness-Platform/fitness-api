package com.fitnessplatform.workout;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkoutExerciseResponse(
        UUID id,
        UUID workoutId,
        UUID exerciseId,
        Integer sets,
        String reps,
        BigDecimal suggestedWeightLb,
        Integer restSeconds,
        String notes,
        Integer position
) {

    public static WorkoutExerciseResponse from(
            WorkoutExercise workoutExercise
    ) {
        return new WorkoutExerciseResponse(
                workoutExercise.getId(),
                workoutExercise.getWorkout().getId(),
                workoutExercise.getExercise().getId(),
                workoutExercise.getSets(),
                workoutExercise.getReps(),
                workoutExercise.getSuggestedWeightLb(),
                workoutExercise.getRestSeconds(),
                workoutExercise.getNotes(),
                workoutExercise.getPosition()
        );
    }
}
