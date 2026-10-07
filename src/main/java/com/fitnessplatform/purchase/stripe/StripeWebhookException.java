package com.fitnessplatform.purchase.stripe;

public class StripeWebhookException extends RuntimeException {

    public StripeWebhookException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }
}
