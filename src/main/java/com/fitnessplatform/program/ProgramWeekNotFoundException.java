package com.fitnessplatform.program;

import java.util.UUID;

public class ProgramWeekNotFoundException extends RuntimeException {

    public ProgramWeekNotFoundException(
            UUID programWeekId
    ) {
        super(
                "Program week not found: " + programWeekId
        );
    }
}
