package com.fitnessplatform.purchase;

import com.fitnessplatform.purchase.stripe.StripeCheckoutGateway;
import com.fitnessplatform.purchase.stripe.StripeCheckoutSession;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PurchaseCheckoutService {

    private final PurchaseCheckoutPersistenceService
            persistenceService;

    private final StripeCheckoutGateway
            stripeCheckoutGateway;

    public PurchaseCheckoutService(
            PurchaseCheckoutPersistenceService persistenceService,
            StripeCheckoutGateway stripeCheckoutGateway
    ) {
        this.persistenceService =
                persistenceService;

        this.stripeCheckoutGateway =
                stripeCheckoutGateway;
    }

    public CheckoutResponse createCheckout(
            UUID userId,
            UUID programId
    ) {
        PendingPurchaseCheckout purchase =
                persistenceService.createPendingPurchase(
                        userId,
                        programId
                );

        StripeCheckoutSession stripeSession =
                stripeCheckoutGateway.createSession(
                        purchase.purchaseId(),
                        purchase.programName(),
                        purchase.amountCents(),
                        purchase.currency()
                );

        persistenceService.attachStripeCheckoutSession(
                purchase.purchaseId(),
                stripeSession.id()
        );

        return new CheckoutResponse(
                purchase.purchaseId(),
                stripeSession.url()
        );
    }
}