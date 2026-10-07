package com.fitnessplatform.purchase.stripe;

import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StripeWebhookServiceTest {

    private static final String WEBHOOK_SECRET =
            "whsec_test_secret";

    private static final String PAYLOAD = """
            {
              "id": "evt_test_123",
              "object": "event",
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "id": "cs_test_123",
                  "object": "checkout.session"
                }
              }
            }
            """;

    @Test
    void shouldVerifyAndParseValidStripeWebhook()
            throws Exception {

        StripeWebhookService service =
                new StripeWebhookService(
                        stripeProperties()
                );

        String signature =
                Webhook.Signature
                        .generateSignatureHeader(
                                PAYLOAD,
                                WEBHOOK_SECRET
                        );

        Event event =
                service.verifyAndParse(
                        PAYLOAD,
                        signature
                );

        assertEquals(
                "evt_test_123",
                event.getId()
        );

        assertEquals(
                "checkout.session.completed",
                event.getType()
        );
    }

    @Test
    void shouldRejectInvalidStripeWebhookSignature() {

        StripeWebhookService service =
                new StripeWebhookService(
                        stripeProperties()
                );

        assertThrows(
                StripeWebhookException.class,
                () ->
                        service.verifyAndParse(
                                PAYLOAD,
                                "invalid-signature"
                        )
        );
    }

    private StripeProperties stripeProperties() {
        return new StripeProperties(
                "sk_test_example",
                WEBHOOK_SECRET,
                new StripeProperties.Checkout(
                        "http://localhost:5173/checkout/success",
                        "http://localhost:5173/checkout/cancel"
                )
        );
    }
}