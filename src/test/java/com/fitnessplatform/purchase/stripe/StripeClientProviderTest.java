package com.fitnessplatform.purchase.stripe;

import com.stripe.StripeClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StripeClientProviderTest {

    @Test
    void shouldRejectMissingStripeSecretKey() {

        StripeProperties properties =
                new StripeProperties(
                        "",
                        "whsec_test",
                        new StripeProperties.Checkout(
                                "http://localhost:5173/checkout/success",
                                "http://localhost:5173/checkout/cancel"
                        )
                );

        StripeClientProvider provider =
                new StripeClientProvider(
                        properties
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        provider::getClient
                );

        assertEquals(
                "Stripe secret key is not configured",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateStripeClientWhenSecretKeyIsConfigured() {

        StripeProperties properties =
                new StripeProperties(
                        "sk_test_example",
                        "whsec_test",
                        new StripeProperties.Checkout(
                                "http://localhost:5173/checkout/success",
                                "http://localhost:5173/checkout/cancel"
                        )
                );

        StripeClientProvider provider =
                new StripeClientProvider(
                        properties
                );

        StripeClient client =
                provider.getClient();

        assertNotNull(
                client
        );
    }
}