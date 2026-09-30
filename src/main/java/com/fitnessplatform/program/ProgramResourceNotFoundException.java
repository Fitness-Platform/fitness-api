package com.fitnessplatform.program;

import java.util.UUID;

public class ProgramResourceNotFoundException extends RuntimeException {

    public ProgramResourceNotFoundException(
            UUID programResourceId
    ) {
        super(
                "Program resource not found: "
                        + programResourceId
        );
    }
}
