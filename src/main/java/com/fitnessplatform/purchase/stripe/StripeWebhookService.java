package com.fitnessplatform.purchase.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookService {

    private final StripeProperties
            stripeProperties;

    public StripeWebhookService(
            StripeProperties stripeProperties
    ) {
        this.stripeProperties =
                stripeProperties;
    }

    public Event verifyAndParse(
            String payload,
            String signatureHeader
    ) {
        try {
            return Webhook.constructEvent(
                    payload,
                    signatureHeader,
                    stripeProperties.webhookSecret()
            );

        } catch (SignatureVerificationException exception) {
            throw new StripeWebhookException(
                    "Invalid Stripe webhook signature",
                    exception
            );
        }
    }
}