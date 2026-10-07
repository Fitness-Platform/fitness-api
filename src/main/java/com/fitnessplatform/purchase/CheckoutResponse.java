package com.fitnessplatform.purchase;

import java.util.UUID;

public record CheckoutResponse(
        UUID purchaseId,
        String checkoutUrl
) {
}
