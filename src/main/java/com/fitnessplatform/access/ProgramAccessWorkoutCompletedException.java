package com.fitnessplatform.access;

public class ProgramAccessWorkoutCompletedException
        extends RuntimeException {

    public ProgramAccessWorkoutCompletedException() {
        super("Workout is already completed");
    }
}