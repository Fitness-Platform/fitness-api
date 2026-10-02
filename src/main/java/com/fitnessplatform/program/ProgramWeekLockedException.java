package com.fitnessplatform.program;

import java.time.Instant;
import java.util.UUID;

public class ProgramWeekLockedException
        extends RuntimeException {

    private final Instant unlocksAt;

    public ProgramWeekLockedException(
            UUID programWeekId,
            Instant unlocksAt
    ) {
        super(
                "Program week is locked: "
                        + programWeekId
                        + ". Unlocks at: "
                        + unlocksAt
        );

        this.unlocksAt =
                unlocksAt;
    }

    public Instant getUnlocksAt() {
        return unlocksAt;
    }
}