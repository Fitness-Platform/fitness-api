package com.fitnessplatform.purchase;

import java.util.UUID;

public record PendingPurchaseCheckout(
        UUID purchaseId,
        String programName,
        Long amountCents,
        String currency
) {
}
