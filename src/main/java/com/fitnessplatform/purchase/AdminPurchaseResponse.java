package com.fitnessplatform.purchase;

import java.time.Instant;
import java.util.UUID;

public record AdminPurchaseResponse(
        UUID purchaseId,
        UUID userId,
        UUID programId,
        String programName,
        PurchaseStatus status,
        Long amountCents,
        String currency,
        Instant createdAt,
        Instant paidAt
) {

    public static AdminPurchaseResponse from(Purchase purchase) {
        return new AdminPurchaseResponse(
                purchase.getId(),
                purchase.getUser().getId(),
                purchase.getProgram().getId(),
                purchase.getProgram().getName(),
                purchase.getStatus(),
                purchase.getAmountCents(),
                purchase.getCurrency(),
                purchase.getCreatedAt(),
                purchase.getPaidAt()
        );
    }
}