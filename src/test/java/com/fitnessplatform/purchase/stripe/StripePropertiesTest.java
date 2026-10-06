package com.fitnessplatform.purchase.stripe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StripePropertiesTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            TestConfiguration.class
                    )
                    .withPropertyValues(
                            "stripe.secret-key=sk_test_example",
                            "stripe.webhook-secret=whsec_example",
                            "stripe.checkout.success-url=http://localhost:5173/checkout/success",
                            "stripe.checkout.cancel-url=http://localhost:5173/checkout/cancel"
                    );

    @Test
    void shouldBindStripeProperties() {

        contextRunner.run(
                context -> {

                    StripeProperties properties =
                            context.getBean(
                                    StripeProperties.class
                            );

                    assertEquals(
                            "sk_test_example",
                            properties.secretKey()
                    );

                    assertEquals(
                            "whsec_example",
                            properties.webhookSecret()
                    );

                    assertEquals(
                            "http://localhost:5173/checkout/success",
                            properties.checkout().successUrl()
                    );

                    assertEquals(
                            "http://localhost:5173/checkout/cancel",
                            properties.checkout().cancelUrl()
                    );
                }
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(
            StripeProperties.class
    )
    static class TestConfiguration {
    }
}