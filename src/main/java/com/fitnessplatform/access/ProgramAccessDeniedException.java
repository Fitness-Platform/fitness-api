package com.fitnessplatform.access;

import java.util.UUID;

public class ProgramAccessDeniedException extends RuntimeException{

    public ProgramAccessDeniedException(
            UUID programId
    ) {
        super(
                "User does not have active access to program: "
                        + programId
        );
    }
}
