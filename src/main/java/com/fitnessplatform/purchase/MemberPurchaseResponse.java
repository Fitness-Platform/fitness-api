package com.fitnessplatform.purchase;

import java.time.Instant;
import java.util.UUID;

public record MemberPurchaseResponse(
        UUID purchaseId,
        UUID programId,
        String programName,
        PurchaseStatus status,
        Long amountCents,
        String currency,
        Instant createdAt,
        Instant paidAt
) {

    public static MemberPurchaseResponse from(Purchase purchase) {
        return new MemberPurchaseResponse(
                purchase.getId(),
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