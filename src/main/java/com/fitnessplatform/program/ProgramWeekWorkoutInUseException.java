package com.fitnessplatform.program;

import java.util.UUID;

public class ProgramWeekWorkoutInUseException
        extends RuntimeException {

    public ProgramWeekWorkoutInUseException(
            UUID programWeekWorkoutId
    ) {
        super(
                "Program week workout %s cannot be deleted because it is already in use"
                        .formatted(
                                programWeekWorkoutId
                        )
        );
    }
}