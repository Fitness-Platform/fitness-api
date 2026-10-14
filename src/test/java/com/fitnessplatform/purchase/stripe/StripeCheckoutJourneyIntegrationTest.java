package com.fitnessplatform.purchase.stripe;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccess;
import com.fitnessplatform.access.ProgramAccessRepository;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.purchase.Purchase;
import com.fitnessplatform.purchase.PurchaseRepository;
import com.fitnessplatform.purchase.PurchaseStatus;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import com.jayway.jsonpath.JsonPath;
import com.stripe.Stripe;
import com.stripe.net.Webhook;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        TestcontainersConfiguration.class,
        StripeCheckoutJourneyIntegrationTest.StripeTestConfiguration.class
})
class StripeCheckoutJourneyIntegrationTest {

    private static final String CHECKOUT_SESSION_ID =
            "cs_test_complete_journey";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ProgramAccessRepository programAccessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StripeProperties stripeProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM stripe_webhook_events");

        programAccessRepository.deleteAll();
        purchaseRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        programRepository.deleteAll();
    }

    @Test
    void shouldCompleteCheckoutAndDeliverPurchasedProgram()
            throws Exception {

        // 1. Create member and purchasable program.

        User member = userRepository.saveAndFlush(
                new User(
                        "journey@example.com",
                        passwordEncoder.encode("StrongPassword123!"),
                        UserRole.USER
                )
        );

        Program program = new Program(
                "Six Week Strength",
                "Strength training program"
        );

        program.updatePrice(8900L);
        program.publish();

        program = programRepository.saveAndFlush(program);

        Cookie authCookie = new Cookie(
                "AUTH_TOKEN",
                jwtService.generateToken(member.getId())
        );

        // 2. Member cannot access the program yet.

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(authCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 3. Member creates a Checkout.

        MvcResult checkoutResult = mockMvc.perform(
                        post(
                                "/api/programs/{programId}/checkout",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(authCookie)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.checkoutUrl")
                                .value(
                                        "https://checkout.stripe.test/journey"
                                )
                )
                .andReturn();

        String checkoutResponse =
                checkoutResult.getResponse().getContentAsString();

        String purchaseIdValue = JsonPath.read(
                checkoutResponse,
                "$.purchaseId"
        );

        UUID purchaseId = UUID.fromString(purchaseIdValue);

        // 4. Verify persisted pending purchase.

        Purchase pendingPurchase = purchaseRepository
                .findById(purchaseId)
                .orElseThrow();

        assertEquals(
                PurchaseStatus.PENDING,
                pendingPurchase.getStatus()
        );

        assertEquals(
                member.getId(),
                pendingPurchase.getUser().getId()
        );

        assertEquals(
                program.getId(),
                pendingPurchase.getProgram().getId()
        );

        assertEquals(
                8900L,
                pendingPurchase.getAmountCents()
        );

        assertEquals(
                "USD",
                pendingPurchase.getCurrency()
        );

        assertEquals(
                CHECKOUT_SESSION_ID,
                pendingPurchase.getStripeCheckoutSessionId()
        );

        assertEquals(0L, programAccessRepository.count());

        // 5. Simulate Stripe payment confirmation.

        String payload = completedSessionPayload(purchaseId);

        String signature =
                Webhook.Signature.generateSignatureHeader(
                        payload,
                        stripeProperties.webhookSecret()
                );

        mockMvc.perform(
                        post("/api/webhooks/stripe")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Stripe-Signature", signature)
                                .content(payload)
                )
                .andExpect(status().isOk());

        // 6. Verify financial state.

        Purchase paidPurchase = purchaseRepository
                .findById(purchaseId)
                .orElseThrow();

        assertEquals(
                PurchaseStatus.PAID,
                paidPurchase.getStatus()
        );

        assertEquals(
                "pi_test_complete_journey",
                paidPurchase.getStripePaymentIntentId()
        );

        assertNotNull(paidPurchase.getPaidAt());

        // 7. Verify access was granted exactly once.

        ProgramAccess access = programAccessRepository
                .findByPurchaseId(purchaseId)
                .orElseThrow();

        assertEquals(member.getId(), access.getUser().getId());

        assertEquals(program.getId(), access.getProgram().getId());

        assertEquals(1L, programAccessRepository.count());

        // 8. Member can now retrieve the paid purchase.

        mockMvc.perform(
                        get(
                                "/api/me/purchases/{purchaseId}",
                                purchaseId
                        )
                                .cookie(authCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.amountCents").value(8900))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty())
                .andExpect(
                        jsonPath("$.stripePaymentIntentId")
                                .doesNotExist()
                );

        // 9. Member can now see the purchased program.

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(authCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].programId")
                                .value(program.getId().toString())
                )
                .andExpect(
                        jsonPath("$[0].name")
                                .value("Six Week Strength")
                );
    }

    private String completedSessionPayload(UUID purchaseId) {
        return """
                {
                  "id": "evt_complete_journey",
                  "object": "event",
                  "api_version": "%s",
                  "created": 1791547200,
                  "livemode": false,
                  "pending_webhooks": 1,
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "checkout.session",
                      "client_reference_id": "%s",
                      "mode": "payment",
                      "payment_status": "paid",
                      "payment_intent": "pi_test_complete_journey"
                    }
                  }
                }
                """.formatted(
                Stripe.API_VERSION,
                CHECKOUT_SESSION_ID,
                purchaseId
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StripeTestConfiguration {

        @Bean
        @Primary
        StripeCheckoutGateway stripeCheckoutGateway() {
            return (
                    purchaseId,
                    programName,
                    amountCents,
                    currency
            ) -> new StripeCheckoutSession(
                    CHECKOUT_SESSION_ID,
                    "https://checkout.stripe.test/journey"
            );
        }
    }
}