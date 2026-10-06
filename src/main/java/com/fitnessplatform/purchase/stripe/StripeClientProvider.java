package com.fitnessplatform.purchase.stripe;

import com.stripe.StripeClient;
import org.springframework.stereotype.Component;

@Component
public class StripeClientProvider {

    private final StripeProperties stripeProperties;

    public StripeClientProvider(
            StripeProperties stripeProperties
    ) {
        this.stripeProperties =
                stripeProperties;
    }

    public StripeClient getClient() {
        String secretKey =
                stripeProperties.secretKey();

        if (
                secretKey == null
                        || secretKey.isBlank()
        ) {
            throw new IllegalStateException(
                    "Stripe secret key is not configured"
            );
        }

        return new StripeClient(
                secretKey
        );
    }
}
