package com.fitnessplatform.exercise;

import java.util.UUID;

public class ExerciseInUseException
        extends RuntimeException {

    public ExerciseInUseException(
            UUID exerciseId
    ) {
        super(
                "Exercise is currently used by one or more workouts: "
                        + exerciseId
        );
    }
}