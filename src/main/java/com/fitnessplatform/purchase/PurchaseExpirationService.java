package com.fitnessplatform.purchase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class PurchaseExpirationService {

    private final PurchaseRepository purchaseRepository;

    public PurchaseExpirationService(
            PurchaseRepository purchaseRepository
    ) {
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional
    public void expirePurchase(
            UUID purchaseId,
            String stripeCheckoutSessionId
    ) {
        Purchase purchase = purchaseRepository
                .findByIdForUpdate(purchaseId)
                .orElseThrow(
                        () -> new PurchasePaymentConfirmationException(
                                "Purchase not found"
                        )
                );

        if (!Objects.equals(
                purchase.getStripeCheckoutSessionId(),
                stripeCheckoutSessionId
        )) {
            throw new PurchasePaymentConfirmationException(
                    "Stripe Checkout Session does not match Purchase"
            );
        }

        if (purchase.getStatus() == PurchaseStatus.PAID) {
            return;
        }

        purchase.markExpired();

        purchaseRepository.saveAndFlush(purchase);
    }
}