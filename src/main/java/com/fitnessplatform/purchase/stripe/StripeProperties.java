package com.fitnessplatform.purchase.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String secretKey,
        String webhookSecret,
        Checkout checkout
) {

    public record Checkout(
            String successUrl,
            String cancelUrl
    ) {
    }
}
