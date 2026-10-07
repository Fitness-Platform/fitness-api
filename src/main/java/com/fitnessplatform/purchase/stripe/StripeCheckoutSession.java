package com.fitnessplatform.purchase.stripe;

public record StripeCheckoutSession(
        String id,
        String url
) {
}
