package com.fitnessplatform.workout;

import java.util.UUID;

public class WorkoutExercisePositionConflictException extends RuntimeException {

    public WorkoutExercisePositionConflictException(
            UUID workoutId,
            Integer position
    ) {
        super(
                "Position %d is already used in workout %s"
        );
    }
}
