package com.fitnessplatform.workout;

import java.util.UUID;

public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(
            UUID workoutId
    ) {
        super(
                "Workout not found: " + workoutId
        );
    }
}
