package com.fitnessplatform.purchase.stripe;

import java.util.UUID;

public interface StripeCheckoutGateway {

    StripeCheckoutSession createSession(
            UUID purchaseId,
            String programName,
            Long amountCents,
            String currency
    );
}
