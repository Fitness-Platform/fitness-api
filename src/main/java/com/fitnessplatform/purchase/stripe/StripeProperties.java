package com.fitnessplatform.purchase.stripe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "stripe")
@Validated
public record StripeProperties(

        @NotBlank
        String secretKey,

        @NotBlank
        String webhookSecret,

        @Valid
        Checkout checkout
) {

    public record Checkout(

            @NotBlank
            String successUrl,

            @NotBlank
            String cancelUrl
    ) {
    }
}
