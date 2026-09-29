package com.fitnessplatform.program;

import java.util.UUID;

public class ProgramWeekWorkoutPositionConflictException extends RuntimeException {

    public ProgramWeekWorkoutPositionConflictException(
            UUID programWeekId,
            Integer position
    ) {
        super(
                "Position %d already used in program week %s"
                        .formatted(
                                position,
                                programWeekId
                        )
        );
    }
}
