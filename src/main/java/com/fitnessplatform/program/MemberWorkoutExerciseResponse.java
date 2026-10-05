package com.fitnessplatform.program;

import com.fitnessplatform.exercise.Exercise;
import com.fitnessplatform.workout.WorkoutExercise;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberWorkoutExerciseResponse(
        UUID workoutExerciseId,
        UUID exerciseId,
        String name,
        String instructions,
        String equipment,
        String videoUrl,
        Integer sets,
        String reps,
        BigDecimal suggestedWeightLb,
        BigDecimal currentWeightLb,
        Integer restSeconds,
        String notes,
        Integer position
) {

    public static MemberWorkoutExerciseResponse from(
            WorkoutExercise workoutExercise,
            BigDecimal currentWeightLb
    ) {
        Exercise exercise =
                workoutExercise.getExercise();

        return new MemberWorkoutExerciseResponse(
                workoutExercise.getId(),
                exercise.getId(),
                exercise.getName(),
                exercise.getInstructions(),
                exercise.getEquipment(),
                exercise.getVideoUrl(),
                workoutExercise.getSets(),
                workoutExercise.getReps(),
                workoutExercise.getSuggestedWeightLb(),
                currentWeightLb,
                workoutExercise.getRestSeconds(),
                workoutExercise.getNotes(),
                workoutExercise.getPosition()
        );
    }
}
