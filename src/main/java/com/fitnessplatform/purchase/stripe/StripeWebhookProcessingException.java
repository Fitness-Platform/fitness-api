package com.fitnessplatform.purchase.stripe;

public class StripeWebhookProcessingException
        extends RuntimeException {

    public StripeWebhookProcessingException(
            String message
    ) {
        super(message);
    }

    public StripeWebhookProcessingException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }
}