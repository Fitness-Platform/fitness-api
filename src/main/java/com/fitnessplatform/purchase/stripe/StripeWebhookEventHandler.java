package com.fitnessplatform.purchase.stripe;

import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StripeWebhookEventHandler {

    private static final String COMPLETED = "checkout.session.completed";
    private static final String EXPIRED = "checkout.session.expired";

    private final StripeWebhookProcessingService processingService;

    public StripeWebhookEventHandler(StripeWebhookProcessingService processingService) {
        this.processingService = processingService;
    }

    public void handle(Event event) {
        String eventType = event.getType();
        if (!COMPLETED.equals(eventType) && !EXPIRED.equals(eventType)) {
            return;
        }

        StripeObject object = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new StripeWebhookProcessingException(
                        "Unable to deserialize Stripe event data"));

        if (!(object instanceof Session session)) {
            throw new StripeWebhookProcessingException(
                    "Stripe event does not contain a Checkout Session");
        }

        if (COMPLETED.equals(eventType) && !"paid".equals(session.getPaymentStatus())) {
            return;
        }

        UUID purchaseId = parsePurchaseId(session.getClientReferenceId());
        String eventId = event.getId();
        if (eventId == null || eventId.isBlank()) {
            throw new StripeWebhookProcessingException("Stripe event has no ID");
        }

        if (EXPIRED.equals(eventType)) {
            processingService.processExpiredCheckout(eventId, purchaseId, session.getId());
            return;
        }

        String paymentIntentId = session.getPaymentIntent();
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new StripeWebhookProcessingException(
                    "Stripe Checkout Session has no PaymentIntent");
        }
        processingService.processPaidCheckout(
                eventId, purchaseId, session.getId(), paymentIntentId
        );
    }

    private UUID parsePurchaseId(String clientReferenceId) {
        if (clientReferenceId == null || clientReferenceId.isBlank()) {
            throw new StripeWebhookProcessingException(
                    "Stripe Checkout Session has no client reference ID");
        }
        try {
            return UUID.fromString(clientReferenceId);
        } catch (IllegalArgumentException exception) {
            throw new StripeWebhookProcessingException(
                    "Invalid Purchase ID in Stripe Checkout Session", exception);
        }
    }
}
