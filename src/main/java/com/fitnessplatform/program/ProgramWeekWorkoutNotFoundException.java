package com.fitnessplatform.program;

import java.util.UUID;

public class ProgramWeekWorkoutNotFoundException extends RuntimeException {

    public ProgramWeekWorkoutNotFoundException(
            UUID programWeekWorkoutId
    ) {
        super(
                "Program week workout not found: " + programWeekWorkoutId
        );
    }
}
