package com.fitnessplatform.program;

import java.util.UUID;

public class ProgramWeekInUseException
        extends RuntimeException {

    public ProgramWeekInUseException(
            UUID programWeekId
    ) {
        super(
                "Program week %s cannot be deleted because it is already in use"
                        .formatted(programWeekId)
        );
    }
}