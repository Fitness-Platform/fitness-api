package com.fitnessplatform.workout;

import java.util.UUID;

public class WorkoutExerciseInUseException
        extends RuntimeException {

    public WorkoutExerciseInUseException(
            UUID workoutExerciseId
    ) {
        super(
                "Workout exercise %s cannot be deleted because it is already in use"
                        .formatted(
                                workoutExerciseId
                        )
        );
    }
}