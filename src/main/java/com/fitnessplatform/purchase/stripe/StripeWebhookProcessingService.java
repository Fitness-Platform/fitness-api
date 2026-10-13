package com.fitnessplatform.purchase.stripe;

import com.fitnessplatform.purchase.PurchaseExpirationService;
import com.fitnessplatform.purchase.PurchasePaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StripeWebhookProcessingService {

    private final StripeWebhookEventRepository eventRepository;
    private final PurchasePaymentService paymentService;
    private final PurchaseExpirationService expirationService;

    public StripeWebhookProcessingService(
            StripeWebhookEventRepository eventRepository,
            PurchasePaymentService paymentService,
            PurchaseExpirationService expirationService
    ) {
        this.eventRepository = eventRepository;
        this.paymentService = paymentService;
        this.expirationService = expirationService;
    }

    @Transactional
    public void processPaidCheckout(
            String eventId,
            UUID purchaseId,
            String checkoutSessionId,
            String paymentIntentId
    ) {
        if (!eventRepository.insertIfAbsent(eventId, "checkout.session.completed")) {
            return;
        }
        paymentService.completeSuccessfulPurchase(
                purchaseId, checkoutSessionId, paymentIntentId
        );
    }

    @Transactional
    public void processExpiredCheckout(
            String eventId,
            UUID purchaseId,
            String checkoutSessionId
    ) {
        if (!eventRepository.insertIfAbsent(eventId, "checkout.session.expired")) {
            return;
        }
        expirationService.expirePurchase(purchaseId, checkoutSessionId);
    }
}
