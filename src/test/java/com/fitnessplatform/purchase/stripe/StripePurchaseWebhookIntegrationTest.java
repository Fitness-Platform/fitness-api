package com.fitnessplatform.purchase.stripe;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.purchase.Purchase;
import com.fitnessplatform.purchase.PurchaseRepository;
import com.fitnessplatform.purchase.PurchaseStatus;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import com.stripe.Stripe;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        TestcontainersConfiguration.class,
        StripePurchaseWebhookIntegrationTest.FixedClockConfiguration.class
})
class StripePurchaseWebhookIntegrationTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-10-09T12:00:00Z"
            );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StripeProperties stripeProperties;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private PasswordResetTokenRepository
            passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        purchaseRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        programRepository.deleteAll();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    NOW,
                    ZoneOffset.UTC
            );
        }
    }

    @Test
    void shouldMarkPurchaseAsPaidFromSuccessfulStripeWebhook()
            throws Exception {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Six Week Strength"
                );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        8900L,
                        "USD"
                );

        purchase.attachStripeCheckoutSession(
                "cs_test_paid_123"
        );

        purchase =
                purchaseRepository.saveAndFlush(
                        purchase
                );

        String payload =
                """
                {
                  "id": "evt_test_paid_123",
                  "object": "event",
                  "api_version": "%s",
                  "created": 1791547200,
                  "livemode": false,
                  "pending_webhooks": 1,
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_test_paid_123",
                      "object": "checkout.session",
                      "client_reference_id": "%s",
                      "mode": "payment",
                      "payment_status": "paid",
                      "payment_intent": "pi_test_paid_123"
                    }
                  }
                }
                """.formatted(
                        Stripe.API_VERSION,
                        purchase.getId()
                );

        String signature =
                Webhook.Signature
                        .generateSignatureHeader(
                                payload,
                                stripeProperties.webhookSecret()
                        );

        mockMvc.perform(
                        post(
                                "/api/webhooks/stripe"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .header(
                                        "Stripe-Signature",
                                        signature
                                )
                                .content(
                                        payload
                                )
                )
                .andExpect(
                        status().isOk()
                );

        Purchase persisted =
                purchaseRepository
                        .findById(
                                purchase.getId()
                        )
                        .orElseThrow();

        assertEquals(
                PurchaseStatus.PAID,
                persisted.getStatus()
        );

        assertEquals(
                NOW,
                persisted.getPaidAt()
        );

        assertEquals(
                "cs_test_paid_123",
                persisted.getStripeCheckoutSessionId()
        );

        assertEquals(
                "pi_test_paid_123",
                persisted.getStripePaymentIntentId()
        );

        assertNotNull(
                persisted.getUpdatedAt()
        );
    }

    private User createUser(
            String email
    ) {
        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(
                                "StrongPassword123!"
                        ),
                        UserRole.USER
                )
        );
    }

    private Program createProgram(
            String name
    ) {
        return programRepository.saveAndFlush(
                new Program(
                        name,
                        null
                )
        );
    }
}