package com.fitnessplatform.purchase.stripe;

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
    private StripeWebhookProcessingService processingService;

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
                new StripeWebhookEventHandler(processingService);
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

        when(event.getId()).thenReturn("evt_test_123");

        handler.handle(
                event
        );

        verify(
                processingService
        ).processPaidCheckout(
                "evt_test_123",
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
                processingService
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
                processingService
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
                processingService
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
                processingService
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
                processingService
        );
    }

    @Test
    void shouldRouteExpiredSessionToProcessingService() {
        UUID purchaseId = UUID.randomUUID();
        Session session = checkoutSession(
                purchaseId.toString(), "cs_test_expired", "unpaid", null
        );
        when(event.getType()).thenReturn("checkout.session.expired");
        when(event.getDataObjectDeserializer()).thenReturn(dataObjectDeserializer);
        when(dataObjectDeserializer.getObject()).thenReturn(Optional.of(session));
        when(event.getId()).thenReturn("evt_expired_123");

        handler.handle(event);

        verify(processingService).processExpiredCheckout(
                "evt_expired_123", purchaseId, "cs_test_expired"
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