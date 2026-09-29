package com.fitnessplatform.workout;

import java.util.UUID;

public class WorkoutInUseException extends RuntimeException {

    public WorkoutInUseException(
            UUID workoutId
    ) {
        super(
                "Workout is currently used by one or more program weeks: "
                         + workoutId
        );
    }
}
