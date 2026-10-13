package com.fitnessplatform.purchase.stripe;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccess;
import com.fitnessplatform.access.ProgramAccessRepository;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.program.ProgramWeek;
import com.fitnessplatform.program.ProgramWeekRepository;
import com.fitnessplatform.purchase.Purchase;
import com.fitnessplatform.purchase.PurchaseRepository;
import com.fitnessplatform.purchase.PurchaseStatus;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import com.stripe.Stripe;
import com.stripe.net.Webhook;
import jakarta.servlet.http.Cookie;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        TestcontainersConfiguration.class,
        StripePurchaseWebhookIntegrationTest.FixedClockConfiguration.class
})
class StripePurchaseWebhookIntegrationTest {

    private static final Instant NOW =
            Instant.parse("2026-10-09T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StripeProperties stripeProperties;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ProgramAccessRepository programAccessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ProgramWeekRepository programWeekRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM stripe_webhook_events");
        programAccessRepository.deleteAll();
        purchaseRepository.deleteAll();
        programWeekRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        programRepository.deleteAll();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Test
    void shouldMarkPurchasePaidAndGrantAccessFromSuccessfulStripeWebhook()
            throws Exception {
        Purchase purchase = createPendingPurchase("cs_test_paid_123");
        String payload = completedSessionPayload(
                purchase.getId(), "cs_test_paid_123", "pi_test_paid_123"
        );

        postSignedWebhook(payload, signatureFor(payload), 200);

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.PAID, persisted.getStatus());
        assertEquals(NOW, persisted.getPaidAt());
        assertEquals("cs_test_paid_123", persisted.getStripeCheckoutSessionId());
        assertEquals("pi_test_paid_123", persisted.getStripePaymentIntentId());
        assertNotNull(persisted.getUpdatedAt());

        ProgramAccess access = programAccessRepository
                .findByPurchaseId(purchase.getId()).orElseThrow();
        assertEquals(purchase.getId(), access.getPurchase().getId());
        assertEquals(purchase.getUser().getId(), access.getUser().getId());
        assertEquals(purchase.getProgram().getId(), access.getProgram().getId());
        assertEquals(NOW, access.getStartsAt());
        assertEquals(Instant.parse("2027-04-09T12:00:00Z"), access.getExpiresAt());
        assertNull(access.getRevokedAt());
        assertEquals(1L, programAccessRepository.count());
    }

    @Test
    void shouldDeliverPurchasedProgramToMemberAfterWebhook()
            throws Exception {
        Purchase purchase = createPendingPurchase("cs_test_delivery_123");
        User buyer = purchase.getUser();
        Program program = purchase.getProgram();

        program.publish();
        programRepository.saveAndFlush(program);

        programWeekRepository.saveAndFlush(
                new ProgramWeek(program, "Week 1", null, 1)
        );
        programWeekRepository.saveAndFlush(
                new ProgramWeek(program, "Week 2", null, 2)
        );

        Cookie memberCookie = new Cookie(
                "AUTH_TOKEN",
                jwtService.generateToken(buyer.getId())
        );

        // The buyer cannot see the program before payment confirmation.
        mockMvc.perform(get("/api/me/programs").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        String payload = completedSessionPayload(
                purchase.getId(),
                "cs_test_delivery_123",
                "pi_test_delivery_123"
        );
        postSignedWebhook(payload, signatureFor(payload), 200);

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.PAID, persisted.getStatus());

        ProgramAccess access = programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow();
        assertEquals(buyer.getId(), access.getUser().getId());
        assertEquals(program.getId(), access.getProgram().getId());

        // The member's own authenticated listing now includes the purchase.
        mockMvc.perform(get("/api/me/programs").cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].programId")
                        .value(program.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Six Week Strength"))
                .andExpect(jsonPath("$[0].accessStartsAt")
                        .value("2026-10-09T12:00:00Z"))
                .andExpect(jsonPath("$[0].accessExpiresAt")
                        .value("2027-04-09T12:00:00Z"));

        // Progressive unlock follows the purchased access start date.
        mockMvc.perform(get("/api/me/programs/{programId}", program.getId())
                        .cookie(memberCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks.length()").value(2))
                .andExpect(jsonPath("$.weeks[0].position").value(1))
                .andExpect(jsonPath("$.weeks[0].unlocked").value(true))
                .andExpect(jsonPath("$.weeks[0].unlocksAt")
                        .value("2026-10-09T12:00:00Z"))
                .andExpect(jsonPath("$.weeks[1].position").value(2))
                .andExpect(jsonPath("$.weeks[1].unlocked").value(false))
                .andExpect(jsonPath("$.weeks[1].unlocksAt")
                        .value("2026-10-16T12:00:00Z"));
    }

    @Test
    void shouldNotDuplicateProgramAccessOnRepeatedStripeWebhook()
            throws Exception {
        Purchase purchase = createPendingPurchase("cs_test_retry_123");
        String payload = completedSessionPayload(
                purchase.getId(), "cs_test_retry_123", "pi_test_retry_123"
        );
        String signature = signatureFor(payload);

        postSignedWebhook(payload, signature, 200);
        UUID originalAccessId = programAccessRepository
                .findByPurchaseId(purchase.getId()).orElseThrow().getId();

        postSignedWebhook(payload, signature, 200);

        ProgramAccess access = programAccessRepository
                .findByPurchaseId(purchase.getId()).orElseThrow();
        assertEquals(originalAccessId, access.getId());
        assertEquals(NOW, access.getStartsAt());
        assertEquals(Instant.parse("2027-04-09T12:00:00Z"), access.getExpiresAt());
        assertEquals(1L, programAccessRepository.count());
    }

    @Test
    void shouldRejectConflictingPaymentIntentWithoutChangingPurchaseOrAccess()
            throws Exception {
        Purchase purchase = createPendingPurchase("cs_test_conflict_123");
        String validPayload = completedSessionPayload(
                purchase.getId(), "cs_test_conflict_123", "pi_test_original"
        );
        postSignedWebhook(validPayload, signatureFor(validPayload), 200);
        UUID originalAccessId = programAccessRepository
                .findByPurchaseId(purchase.getId()).orElseThrow().getId();

        String conflictingPayload = completedSessionPayload(
                purchase.getId(), "cs_test_conflict_123", "pi_test_different"
        );
        postSignedWebhook(conflictingPayload, signatureFor(conflictingPayload), 500);

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.PAID, persisted.getStatus());
        assertEquals(NOW, persisted.getPaidAt());
        assertEquals("pi_test_original", persisted.getStripePaymentIntentId());
        assertEquals(originalAccessId, programAccessRepository
                .findByPurchaseId(purchase.getId()).orElseThrow().getId());
        assertEquals(1L, programAccessRepository.count());
    }

    @Test
    void shouldRejectInvalidSignatureWithoutProcessingPayment()
            throws Exception {
        Purchase purchase = createPendingPurchase("cs_test_bad_signature");
        String payload = completedSessionPayload(
                purchase.getId(), "cs_test_bad_signature", "pi_test_bad_signature"
        );
        // Signature generated for a different body: should fail verification.
        String invalidSignature = signatureFor("different payload");

        postSignedWebhook(payload, invalidSignature, 400);

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.PENDING, persisted.getStatus());
        assertNull(persisted.getPaidAt());
        assertNull(persisted.getStripePaymentIntentId());
        assertTrue(programAccessRepository.findByPurchaseId(purchase.getId()).isEmpty());
    }

    @Test
    void shouldExpirePendingPurchaseFromStripeWebhook() throws Exception {
        Purchase purchase = createPendingPurchase("cs_test_expiration_123");
        String payload = expiredSessionPayload(
                purchase.getId(), "cs_test_expiration_123"
        );

        postSignedWebhook(payload, signatureFor(payload), 200);

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.EXPIRED, persisted.getStatus());
        assertNull(persisted.getPaidAt());
        assertNull(persisted.getStripePaymentIntentId());
        assertEquals("cs_test_expiration_123", persisted.getStripeCheckoutSessionId());
        assertTrue(programAccessRepository.findByPurchaseId(purchase.getId()).isEmpty());
    }

    @Test
    void shouldNotChangeExpiredPurchaseOnRepeatedExpirationWebhook() throws Exception {
        Purchase purchase = createPendingPurchase("cs_test_expiration_retry");
        String payload = expiredSessionPayload(
                purchase.getId(), "cs_test_expiration_retry"
        );
        String signature = signatureFor(payload);

        postSignedWebhook(payload, signature, 200);
        postSignedWebhook(payload, signature, 200);

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.EXPIRED, persisted.getStatus());
        assertNull(persisted.getPaidAt());
        assertNull(persisted.getStripePaymentIntentId());
        assertTrue(programAccessRepository.findByPurchaseId(purchase.getId()).isEmpty());
    }

    @Test
    void shouldNotExpirePaidPurchaseOrRevokeAccess() throws Exception {
        Purchase purchase = createPendingPurchase("cs_test_paid_then_expired");
        String completedPayload = completedSessionPayload(
                purchase.getId(), "cs_test_paid_then_expired", "pi_test_paid_then_expired"
        );
        postSignedWebhook(completedPayload, signatureFor(completedPayload), 200);

        UUID accessId = programAccessRepository.findByPurchaseId(purchase.getId())
                .orElseThrow().getId();

        String expiredPayload = expiredSessionPayload(
                purchase.getId(), "cs_test_paid_then_expired"
        );
        postSignedWebhook(expiredPayload, signatureFor(expiredPayload), 200);

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.PAID, persisted.getStatus());
        assertEquals(NOW, persisted.getPaidAt());
        assertEquals("pi_test_paid_then_expired", persisted.getStripePaymentIntentId());

        ProgramAccess access = programAccessRepository.findByPurchaseId(purchase.getId())
                .orElseThrow();
        assertEquals(accessId, access.getId());
        assertNull(access.getRevokedAt());
        assertEquals(1L, programAccessRepository.count());
    }

    @Test
    void shouldRejectExpirationWithMismatchedCheckoutSession() throws Exception {
        Purchase purchase = createPendingPurchase("cs_test_expected");
        String payload = expiredSessionPayload(
                purchase.getId(), "cs_test_unexpected"
        );

        postSignedWebhook(payload, signatureFor(payload), 500);

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.PENDING, persisted.getStatus());
        assertEquals("cs_test_expected", persisted.getStripeCheckoutSessionId());
        assertNull(persisted.getPaidAt());
        assertNull(persisted.getStripePaymentIntentId());
        assertTrue(programAccessRepository.findByPurchaseId(purchase.getId()).isEmpty());
    }

    @Test
    void shouldRejectExpirationForUnknownPurchase() throws Exception {
        String payload = expiredSessionPayload(
                UUID.randomUUID(), "cs_test_unknown_purchase"
        );

        postSignedWebhook(payload, signatureFor(payload), 500);
        assertEquals(0L, purchaseRepository.count());
        assertEquals(0L, programAccessRepository.count());
    }

    private void postSignedWebhook(String payload, String signature, int expectedStatus)
            throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", signature)
                        .content(payload))
                .andExpect(status().is(expectedStatus));
    }

    private String signatureFor(String payload)
            throws Exception {

        return Webhook.Signature.generateSignatureHeader(
                payload,
                stripeProperties.webhookSecret()
        );
    }

    private String expiredSessionPayload(UUID purchaseId, String checkoutSessionId) {
        return """
                {
                  "id": "evt_expired_%s",
                  "object": "event",
                  "api_version": "%s",
                  "created": 1791547200,
                  "livemode": false,
                  "pending_webhooks": 1,
                  "type": "checkout.session.expired",
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "checkout.session",
                      "client_reference_id": "%s",
                      "mode": "payment",
                      "status": "expired",
                      "payment_status": "unpaid"
                    }
                  }
                }
                """.formatted(checkoutSessionId, Stripe.API_VERSION, checkoutSessionId, purchaseId);
    }

    private String completedSessionPayload(
            UUID purchaseId, String checkoutSessionId, String paymentIntentId
    ) {
        return """
                {
                  "id": "evt_completed_%s",
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
                      "payment_intent": "%s"
                    }
                  }
                }
                """.formatted(
                paymentIntentId,
                Stripe.API_VERSION,
                checkoutSessionId,
                purchaseId,
                paymentIntentId
        );
    }

    private Purchase createPendingPurchase(String checkoutSessionId) {
        User user = createUser("member@example.com");
        Program program = createProgram("Six Week Strength");
        Purchase purchase = new Purchase(user, program, 8900L, "USD");
        purchase.attachStripeCheckoutSession(checkoutSessionId);
        return purchaseRepository.saveAndFlush(purchase);
    }

    private User createUser(String email) {
        return userRepository.saveAndFlush(
                new User(email, passwordEncoder.encode("StrongPassword123!"), UserRole.USER)
        );
    }

    private Program createProgram(String name) {
        return programRepository.saveAndFlush(new Program(name, null));
    }
}
