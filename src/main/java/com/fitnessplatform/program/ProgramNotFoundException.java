package com.fitnessplatform.program;

import java.util.UUID;

public class ProgramNotFoundException extends RuntimeException {

    public ProgramNotFoundException(
            UUID programId
    ) {
        super(
                "Program not found with: " + programId
        );
    }
}
