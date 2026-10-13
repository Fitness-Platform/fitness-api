package com.fitnessplatform.purchase.stripe;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccess;
import com.fitnessplatform.access.ProgramAccessRepository;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.purchase.Purchase;
import com.fitnessplatform.purchase.PurchasePaymentConfirmationException;
import com.fitnessplatform.purchase.PurchaseRepository;
import com.fitnessplatform.purchase.PurchaseStatus;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StripeWebhookProcessingIntegrationTest {

    @Autowired
    private StripeWebhookProcessingService processingService;

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
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM stripe_webhook_events");
        programAccessRepository.deleteAll();
        purchaseRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        programRepository.deleteAll();
    }

    @Test
    void shouldRollBackEventWhenPurchaseConfirmationFails() {
        Purchase purchase = createPendingPurchase("cs_test_expected");

        assertThrows(
                PurchasePaymentConfirmationException.class,
                () -> processingService.processPaidCheckout(
                        "evt_test_rollback",
                        purchase.getId(),
                        "cs_test_wrong",
                        "pi_test_rollback"
                )
        );

        assertEquals(0L, countEvents());

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.PENDING, persisted.getStatus());
        assertNull(persisted.getPaidAt());
        assertNull(persisted.getStripePaymentIntentId());
        assertTrue(programAccessRepository.findByPurchaseId(purchase.getId()).isEmpty());
    }

    @Test
    void shouldAllowRetryOfSameEventIdAfterRollback() {
        Purchase purchase = createPendingPurchase("cs_test_retry");
        String eventId = "evt_test_retry_after_failure";

        assertThrows(
                PurchasePaymentConfirmationException.class,
                () -> processingService.processPaidCheckout(
                        eventId,
                        purchase.getId(),
                        "cs_test_wrong",
                        "pi_test_retry"
                )
        );

        assertEquals(0L, countEvents());

        // Test harness corrects the invalid correlation on the second attempt.
        processingService.processPaidCheckout(
                eventId,
                purchase.getId(),
                "cs_test_retry",
                "pi_test_retry"
        );

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.PAID, persisted.getStatus());
        assertNotNull(persisted.getPaidAt());
        assertEquals("pi_test_retry", persisted.getStripePaymentIntentId());

        ProgramAccess access = programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow();
        assertEquals(purchase.getId(), access.getPurchase().getId());
        assertEquals(1L, programAccessRepository.count());
        assertEquals(1L, countEvents());
    }

    @Test
    void shouldSkipAlreadyProcessedEventWithoutChangingPaymentOrAccess() {
        Purchase purchase = createPendingPurchase("cs_test_duplicate");
        String eventId = "evt_test_duplicate";

        processingService.processPaidCheckout(
                eventId,
                purchase.getId(),
                "cs_test_duplicate",
                "pi_test_original"
        );

        Purchase first = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        UUID accessId = programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow()
                .getId();

        // Deliberately conflicting arguments prove this event was short-circuited.
        processingService.processPaidCheckout(
                eventId,
                purchase.getId(),
                "cs_test_duplicate",
                "pi_test_different"
        );

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        assertEquals(PurchaseStatus.PAID, persisted.getStatus());
        assertEquals(first.getPaidAt(), persisted.getPaidAt());
        assertEquals("pi_test_original", persisted.getStripePaymentIntentId());
        assertEquals(accessId, programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow()
                .getId());
        assertEquals(1L, programAccessRepository.count());
        assertEquals(1L, countEvents());
    }

    private Purchase createPendingPurchase(String checkoutSessionId) {
        User user = userRepository.saveAndFlush(
                new User(
                        "member@example.com",
                        passwordEncoder.encode("StrongPassword123!"),
                        UserRole.USER
                )
        );
        Program program = programRepository.saveAndFlush(
                new Program("Six Week Strength", null)
        );
        Purchase purchase = new Purchase(user, program, 8900L, "USD");
        purchase.attachStripeCheckoutSession(checkoutSessionId);
        return purchaseRepository.saveAndFlush(purchase);
    }

    private long countEvents() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stripe_webhook_events",
                Long.class
        );
        return count == null ? 0L : count;
    }
}
