package com.fitnessplatform.access;

public class ProgramAccessWorkoutIncompleteException
        extends RuntimeException {

    public ProgramAccessWorkoutIncompleteException() {
        super(
                "All prescribed sets must be completed before completing the workout"
        );
    }
}