package com.fitnessplatform.exercise;

import java.util.UUID;

public class ExerciseNotFoundException extends RuntimeException {

    public ExerciseNotFoundException(UUID exerciseId) {
        super("Exercise not found with id: " + exerciseId);
    }
}
