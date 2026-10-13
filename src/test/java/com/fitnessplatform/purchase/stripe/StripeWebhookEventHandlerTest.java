package com.fitnessplatform.purchase.stripe;

import com.fitnessplatform.purchase.PurchasePaymentService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookEventHandlerTest {

    @Mock
    private PurchasePaymentService
            purchasePaymentService;

    @Mock
    private Event
            event;

    @Mock
    private EventDataObjectDeserializer
            dataObjectDeserializer;

    private StripeWebhookEventHandler
            handler;

    @BeforeEach
    void setUp() {
        handler =
                new StripeWebhookEventHandler(
                        purchasePaymentService
                );
    }

    @Test
    void shouldCompletePaidCheckoutSession() {

        UUID purchaseId =
                UUID.randomUUID();

        Session session =
                checkoutSession(
                        purchaseId.toString(),
                        "cs_test_123",
                        "paid",
                        "pi_test_123"
                );

        when(
                event.getType()
        ).thenReturn(
                "checkout.session.completed"
        );

        when(
                event.getDataObjectDeserializer()
        ).thenReturn(
                dataObjectDeserializer
        );

        when(
                dataObjectDeserializer.getObject()
        ).thenReturn(
                Optional.<StripeObject>of(
                        session
                )
        );

        handler.handle(
                event
        );

        verify(
                purchasePaymentService
        ).completeSuccessfulPurchase(
                purchaseId,
                "cs_test_123",
                "pi_test_123"
        );
    }

    @Test
    void shouldIgnoreUnpaidCheckoutSession() {

        Session session =
                checkoutSession(
                        UUID.randomUUID()
                                .toString(),
                        "cs_test_123",
                        "unpaid",
                        "pi_test_123"
                );

        when(
                event.getType()
        ).thenReturn(
                "checkout.session.completed"
        );

        when(
                event.getDataObjectDeserializer()
        ).thenReturn(
                dataObjectDeserializer
        );

        when(
                dataObjectDeserializer.getObject()
        ).thenReturn(
                Optional.<StripeObject>of(
                        session
                )
        );

        handler.handle(
                event
        );

        verifyNoInteractions(
                purchasePaymentService
        );
    }

    @Test
    void shouldIgnoreUnsupportedEvent() {

        when(
                event.getType()
        ).thenReturn(
                "customer.created"
        );

        handler.handle(
                event
        );

        verifyNoInteractions(
                purchasePaymentService
        );

        verify(
                event,
                never()
        ).getDataObjectDeserializer();
    }

    @Test
    void shouldRejectInvalidPurchaseId() {

        Session session =
                checkoutSession(
                        "not-a-uuid",
                        "cs_test_123",
                        "paid",
                        "pi_test_123"
                );

        when(
                event.getType()
        ).thenReturn(
                "checkout.session.completed"
        );

        when(
                event.getDataObjectDeserializer()
        ).thenReturn(
                dataObjectDeserializer
        );

        when(
                dataObjectDeserializer.getObject()
        ).thenReturn(
                Optional.<StripeObject>of(
                        session
                )
        );

        assertThrows(
                StripeWebhookProcessingException.class,
                () ->
                        handler.handle(
                                event
                        )
        );

        verifyNoInteractions(
                purchasePaymentService
        );
    }

    @Test
    void shouldRejectCheckoutWithoutPaymentIntent() {

        Session session =
                checkoutSession(
                        UUID.randomUUID()
                                .toString(),
                        "cs_test_123",
                        "paid",
                        null
                );

        when(
                event.getType()
        ).thenReturn(
                "checkout.session.completed"
        );

        when(
                event.getDataObjectDeserializer()
        ).thenReturn(
                dataObjectDeserializer
        );

        when(
                dataObjectDeserializer.getObject()
        ).thenReturn(
                Optional.<StripeObject>of(
                        session
                )
        );

        assertThrows(
                StripeWebhookProcessingException.class,
                () ->
                        handler.handle(
                                event
                        )
        );

        verifyNoInteractions(
                purchasePaymentService
        );
    }

    @Test
    void shouldRejectUndeserializableEventData() {

        when(
                event.getType()
        ).thenReturn(
                "checkout.session.completed"
        );

        when(
                event.getDataObjectDeserializer()
        ).thenReturn(
                dataObjectDeserializer
        );

        when(
                dataObjectDeserializer.getObject()
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                StripeWebhookProcessingException.class,
                () ->
                        handler.handle(
                                event
                        )
        );

        verifyNoInteractions(
                purchasePaymentService
        );
    }

    private Session checkoutSession(
            String clientReferenceId,
            String sessionId,
            String paymentStatus,
            String paymentIntentId
    ) {
        Session session =
                new Session();

        session.setId(
                sessionId
        );

        session.setClientReferenceId(
                clientReferenceId
        );

        session.setPaymentStatus(
                paymentStatus
        );

        if (
                paymentIntentId != null
        ) {
            session.setPaymentIntent(
                    paymentIntentId
            );
        }

        return session;
    }
}