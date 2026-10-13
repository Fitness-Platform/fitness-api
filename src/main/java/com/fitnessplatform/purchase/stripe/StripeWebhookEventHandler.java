package com.fitnessplatform.purchase.stripe;

import com.fitnessplatform.purchase.PurchasePaymentService;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StripeWebhookEventHandler {

    private static final String CHECKOUT_SESSION_COMPLETED =
            "checkout.session.completed";

    private static final String PAYMENT_STATUS_PAID =
            "paid";

    private final PurchasePaymentService
            purchasePaymentService;

    public StripeWebhookEventHandler(
            PurchasePaymentService purchasePaymentService
    ) {
        this.purchasePaymentService =
                purchasePaymentService;
    }

    public void handle(
            Event event
    ) {
        if (
                !CHECKOUT_SESSION_COMPLETED.equals(
                        event.getType()
                )
        ) {
            return;
        }

        StripeObject stripeObject =
                event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(
                                () ->
                                        new StripeWebhookProcessingException(
                                                "Unable to deserialize Stripe event data"
                                        )
                        );

        if (!(stripeObject instanceof Session session)) {
            throw new StripeWebhookProcessingException(
                    "Stripe event does not contain a Checkout Session"
            );
        }

        if (
                !PAYMENT_STATUS_PAID.equals(
                        session.getPaymentStatus()
                )
        ) {
            return;
        }

        UUID purchaseId =
                parsePurchaseId(
                        session.getClientReferenceId()
                );

        String paymentIntentId =
                session.getPaymentIntent();

        if (
                paymentIntentId == null
                        || paymentIntentId.isBlank()
        ) {
            throw new StripeWebhookProcessingException(
                    "Stripe Checkout Session has no PaymentIntent"
            );
        }

        purchasePaymentService
                .completeSuccessfulPurchase(
                        purchaseId,
                        session.getId(),
                        paymentIntentId
                );
    }

    private UUID parsePurchaseId(
            String clientReferenceId
    ) {
        if (
                clientReferenceId == null
                        || clientReferenceId.isBlank()
        ) {
            throw new StripeWebhookProcessingException(
                    "Stripe Checkout Session has no client reference ID"
            );
        }

        try {
            return UUID.fromString(
                    clientReferenceId
            );

        } catch (IllegalArgumentException exception) {
            throw new StripeWebhookProcessingException(
                    "Invalid Purchase ID in Stripe Checkout Session",
                    exception
            );
        }
    }
}