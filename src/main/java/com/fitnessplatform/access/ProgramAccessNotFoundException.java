package com.fitnessplatform.access;

import java.util.UUID;

public class ProgramAccessNotFoundException extends RuntimeException {

    public ProgramAccessNotFoundException(
            UUID accessId
    ) {
        super(
                "Program access not found: "
                        + accessId
        );
    }
}
