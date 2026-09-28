package com.fitnessplatform.workout;

import java.util.UUID;

public class WorkoutExerciseNotFoundException extends RuntimeException {

    public WorkoutExerciseNotFoundException(
            UUID workoutExerciseId
    ) {
        super(
                "Workout exercise not found: " + workoutExerciseId
        );
    }
}
