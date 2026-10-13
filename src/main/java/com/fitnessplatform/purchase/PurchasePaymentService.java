package com.fitnessplatform.purchase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@Service
public class PurchasePaymentService {

    private final PurchaseRepository
            purchaseRepository;

    private final Clock
            clock;

    public PurchasePaymentService(
            PurchaseRepository purchaseRepository,
            Clock clock
    ) {
        this.purchaseRepository =
                purchaseRepository;

        this.clock =
                clock;
    }

    @Transactional
    public void completeSuccessfulPurchase(
            UUID purchaseId,
            String stripeCheckoutSessionId,
            String stripePaymentIntentId
    ) {
        Purchase purchase =
                purchaseRepository
                        .findById(
                                purchaseId
                        )
                        .orElseThrow(
                                () ->
                                        new PurchasePaymentConfirmationException(
                                                "Purchase not found"
                                        )
                        );

        validateCheckoutSession(
                purchase,
                stripeCheckoutSessionId
        );

        if (
                purchase.getStatus()
                        == PurchaseStatus.PAID
        ) {
            validateRepeatedConfirmation(
                    purchase,
                    stripePaymentIntentId
            );

            return;
        }

        if (
                purchase.getStatus()
                        != PurchaseStatus.PENDING
        ) {
            throw new PurchasePaymentConfirmationException(
                    "Purchase is not pending"
            );
        }

        purchase.markPaid(
                clock.instant(),
                stripePaymentIntentId
        );

        purchaseRepository.saveAndFlush(
                purchase
        );
    }

    private void validateCheckoutSession(
            Purchase purchase,
            String stripeCheckoutSessionId
    ) {
        if (
                !Objects.equals(
                        purchase.getStripeCheckoutSessionId(),
                        stripeCheckoutSessionId
                )
        ) {
            throw new PurchasePaymentConfirmationException(
                    "Stripe Checkout Session does not match Purchase"
            );
        }
    }

    private void validateRepeatedConfirmation(
            Purchase purchase,
            String stripePaymentIntentId
    ) {
        if (
                !Objects.equals(
                        purchase.getStripePaymentIntentId(),
                        stripePaymentIntentId
                )
        ) {
            throw new PurchasePaymentConfirmationException(
                    "Stripe PaymentIntent does not match paid Purchase"
            );
        }
    }
}