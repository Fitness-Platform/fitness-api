package com.fitnessplatform.program;

import java.util.UUID;

public class ProgramWeekPositionConflictException extends RuntimeException {

    public ProgramWeekPositionConflictException(
            UUID programWeekId,
            Integer position
    ) {
        super(
                "Position %d is already used in program %s"
                        .formatted(
                                position,
                                programWeekId
                        )
        );
    }
}
