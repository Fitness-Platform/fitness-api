package com.fitnessplatform.access;

public class ProgramAccessInvalidPeriodException extends RuntimeException {

    public ProgramAccessInvalidPeriodException() {
        super(
                "Program access expiration must be after its start time."
        );
    }
}
