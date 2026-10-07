package com.fitnessplatform.purchase;

import java.util.UUID;

public class ProgramNotPurchasableException extends RuntimeException {

    public ProgramNotPurchasableException(
            UUID programId,
            String reason
    ) {
        super(
                "Program cannot be purchased: "
                        + programId
                        + ". "
                        + reason
        );
    }
}
