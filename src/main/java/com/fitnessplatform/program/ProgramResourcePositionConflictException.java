package com.fitnessplatform.program;

import java.util.UUID;

public class ProgramResourcePositionConflictException extends RuntimeException {

    public ProgramResourcePositionConflictException(
            UUID programResources,
            Integer position
    ) {
        super(
                "Position %d is already used in program %s"
                        .formatted(
                                position,
                                programResources
                        )
        );
    }
}
