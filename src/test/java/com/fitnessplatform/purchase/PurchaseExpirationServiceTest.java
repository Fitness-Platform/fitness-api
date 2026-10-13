package com.fitnessplatform.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseExpirationServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    private PurchaseExpirationService expirationService;

    private UUID purchaseId;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        expirationService = new PurchaseExpirationService(
                purchaseRepository
        );

        purchaseId = UUID.randomUUID();

        purchase = new Purchase(
                null,
                null,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession(
                "cs_test_123"
        );
    }

    @Test
    void shouldExpirePendingPurchase() {
        when(purchaseRepository.findByIdForUpdate(purchaseId))
                .thenReturn(Optional.of(purchase));

        expirationService.expirePurchase(
                purchaseId,
                "cs_test_123"
        );

        assertEquals(
                PurchaseStatus.EXPIRED,
                purchase.getStatus()
        );

        assertNull(purchase.getPaidAt());
        assertNull(purchase.getStripePaymentIntentId());

        verify(purchaseRepository)
                .saveAndFlush(purchase);
    }

    @Test
    void shouldIgnoreExpirationForPaidPurchase() {
        Instant paidAt = Instant.parse(
                "2026-10-09T12:00:00Z"
        );

        purchase.markPaid(
                paidAt,
                "pi_test_123"
        );

        when(purchaseRepository.findByIdForUpdate(purchaseId))
                .thenReturn(Optional.of(purchase));

        expirationService.expirePurchase(
                purchaseId,
                "cs_test_123"
        );

        assertEquals(
                PurchaseStatus.PAID,
                purchase.getStatus()
        );

        assertEquals(paidAt, purchase.getPaidAt());

        assertEquals(
                "pi_test_123",
                purchase.getStripePaymentIntentId()
        );

        verify(purchaseRepository)
                .findByIdForUpdate(purchaseId);

        verify(purchaseRepository, never())
                .saveAndFlush(any(Purchase.class));
    }

    @Test
    void shouldAllowRepeatedExpiration() {
        purchase.markExpired();

        when(purchaseRepository.findByIdForUpdate(purchaseId))
                .thenReturn(Optional.of(purchase));

        expirationService.expirePurchase(
                purchaseId,
                "cs_test_123"
        );

        assertEquals(
                PurchaseStatus.EXPIRED,
                purchase.getStatus()
        );

        assertNull(purchase.getPaidAt());
        assertNull(purchase.getStripePaymentIntentId());
    }

    @Test
    void shouldRejectMismatchedCheckoutSession() {
        when(purchaseRepository.findByIdForUpdate(purchaseId))
                .thenReturn(Optional.of(purchase));

        assertThrows(
                PurchasePaymentConfirmationException.class,
                () -> expirationService.expirePurchase(
                        purchaseId,
                        "cs_test_different"
                )
        );

        assertEquals(
                PurchaseStatus.PENDING,
                purchase.getStatus()
        );

        verify(purchaseRepository, never())
                .saveAndFlush(any(Purchase.class));
    }

    @Test
    void shouldRejectUnknownPurchase() {
        when(purchaseRepository.findByIdForUpdate(purchaseId))
                .thenReturn(Optional.empty());

        assertThrows(
                PurchasePaymentConfirmationException.class,
                () -> expirationService.expirePurchase(
                        purchaseId,
                        "cs_test_123"
                )
        );

        verify(purchaseRepository, never())
                .saveAndFlush(any(Purchase.class));
    }
}