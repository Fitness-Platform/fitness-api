package com.fitnessplatform.purchase.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class StripeCheckoutGatewayImpl
        implements StripeCheckoutGateway {

    private final StripeClientProvider
            stripeClientProvider;

    private final StripeProperties
            stripeProperties;

    public StripeCheckoutGatewayImpl(
            StripeClientProvider stripeClientProvider,
            StripeProperties stripeProperties
    ) {
        this.stripeClientProvider =
                stripeClientProvider;

        this.stripeProperties =
                stripeProperties;
    }

    @Override
    public StripeCheckoutSession createSession(
            UUID purchaseId,
            String programName,
            Long amountCents,
            String currency
    ) {
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(
                                SessionCreateParams.Mode.PAYMENT
                        )
                        .setSuccessUrl(
                                stripeProperties
                                        .checkout()
                                        .successUrl()
                        )
                        .setCancelUrl(
                                stripeProperties
                                        .checkout()
                                        .cancelUrl()
                        )
                        .setClientReferenceId(
                                purchaseId.toString()
                        )
                        .putMetadata(
                                "purchaseId",
                                purchaseId.toString()
                        )
                        .addLineItem(
                                SessionCreateParams.LineItem
                                        .builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams
                                                        .LineItem
                                                        .PriceData
                                                        .builder()
                                                        .setCurrency(
                                                                currency.toLowerCase(
                                                                        Locale.ROOT
                                                                )
                                                        )
                                                        .setUnitAmount(
                                                                amountCents
                                                        )
                                                        .setProductData(
                                                                SessionCreateParams
                                                                        .LineItem
                                                                        .PriceData
                                                                        .ProductData
                                                                        .builder()
                                                                        .setName(
                                                                                programName
                                                                        )
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

        try {
            Session session =
                    stripeClientProvider
                            .getClient()
                            .v1()
                            .checkout()
                            .sessions()
                            .create(
                                    params
                            );

            return new StripeCheckoutSession(
                    session.getId(),
                    session.getUrl()
            );

        } catch (StripeException exception) {
            throw new StripeCheckoutException(
                    "Unable to create Stripe Checkout Session",
                    exception
            );
        }
    }
}