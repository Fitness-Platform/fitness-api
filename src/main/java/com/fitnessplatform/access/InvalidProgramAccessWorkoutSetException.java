package com.fitnessplatform.access;

public class InvalidProgramAccessWorkoutSetException
        extends RuntimeException {

    public InvalidProgramAccessWorkoutSetException(
            String message
    ) {
        super(message);
    }
}