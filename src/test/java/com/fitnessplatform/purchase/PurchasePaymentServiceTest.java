package com.fitnessplatform.purchase;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccess;
import com.fitnessplatform.access.ProgramAccessRepository;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import({
        TestcontainersConfiguration.class,
        PurchasePaymentServiceTest.FixedClockConfiguration.class
})
class PurchasePaymentServiceTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-10-09T12:00:00Z"
            );

    @Autowired
    private PurchasePaymentService
            purchasePaymentService;

    @Autowired
    private PurchaseRepository
            purchaseRepository;

    @Autowired
    private UserRepository
            userRepository;

    @Autowired
    private ProgramRepository
            programRepository;

    @Autowired
    private PasswordResetTokenRepository
            passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder
            passwordEncoder;

    @Autowired
    private ProgramAccessRepository programAccessRepository;

    @BeforeEach
    void cleanDatabase() {
        programAccessRepository.deleteAll();
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
    void shouldCompletePendingPurchaseSuccessfully() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Six Week Strength"
                );

        Purchase purchase =
                createPendingPurchase(
                        user,
                        program,
                        "cs_test_123"
                );

        purchasePaymentService
                .completeSuccessfulPurchase(
                        purchase.getId(),
                        "cs_test_123",
                        "pi_test_123"
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
                "pi_test_123",
                persisted.getStripePaymentIntentId()
        );

        assertEquals(
                "cs_test_123",
                persisted.getStripeCheckoutSessionId()
        );

        ProgramAccess access = programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow();

        assertEquals(purchase.getId(), access.getPurchase().getId());
        assertEquals(user.getId(), access.getUser().getId());
        assertEquals(program.getId(), access.getProgram().getId());
        assertEquals(NOW, access.getStartsAt());
        assertEquals(
                Instant.parse("2027-04-09T12:00:00Z"),
                access.getExpiresAt()
        );
        assertNull(access.getRevokedAt());
    }

    @Test
    void shouldAllowRepeatedSuccessfulConfirmation() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Six Week Strength"
                );

        Purchase purchase =
                createPendingPurchase(
                        user,
                        program,
                        "cs_test_123"
                );

        purchasePaymentService
                .completeSuccessfulPurchase(
                        purchase.getId(),
                        "cs_test_123",
                        "pi_test_123"
                );

        ProgramAccess initialAccess = programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow();
        UUID initialAccessId = initialAccess.getId();

        purchasePaymentService
                .completeSuccessfulPurchase(
                        purchase.getId(),
                        "cs_test_123",
                        "pi_test_123"
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
                "pi_test_123",
                persisted.getStripePaymentIntentId()
        );

        assertEquals(
                "cs_test_123",
                persisted.getStripeCheckoutSessionId()
        );

        ProgramAccess persistedAccess = programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow();

        assertEquals(initialAccessId, persistedAccess.getId());
        assertEquals(NOW, persistedAccess.getStartsAt());
        assertEquals(
                Instant.parse("2027-04-09T12:00:00Z"),
                persistedAccess.getExpiresAt()
        );
        assertEquals(1L, programAccessRepository.count());
    }

    @Test
    void shouldRejectMismatchedCheckoutSession() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Six Week Strength"
                );

        Purchase purchase =
                createPendingPurchase(
                        user,
                        program,
                        "cs_test_123"
                );

        assertThrows(
                PurchasePaymentConfirmationException.class,
                () ->
                        purchasePaymentService
                                .completeSuccessfulPurchase(
                                        purchase.getId(),
                                        "cs_test_other",
                                        "pi_test_123"
                                )
        );

        Purchase persisted =
                purchaseRepository
                        .findById(
                                purchase.getId()
                        )
                        .orElseThrow();

        assertEquals(
                PurchaseStatus.PENDING,
                persisted.getStatus()
        );

        assertNull(
                persisted.getPaidAt()
        );

        assertNull(
                persisted.getStripePaymentIntentId()
        );

        assertTrue(programAccessRepository
                .findByPurchaseId(purchase.getId())
                .isEmpty());
    }

    @Test
    void shouldRejectUnknownPurchase() {

        assertThrows(
                PurchasePaymentConfirmationException.class,
                () ->
                        purchasePaymentService
                                .completeSuccessfulPurchase(
                                        UUID.randomUUID(),
                                        "cs_test_123",
                                        "pi_test_123"
                                )
        );

        assertEquals(0L, programAccessRepository.count());
    }

    @Test
    void shouldRejectRepeatedConfirmationWithDifferentPaymentIntent() {
        User user = createUser("member@example.com");
        Program program = createProgram("Six Week Strength");
        Purchase purchase = createPendingPurchase(user, program, "cs_test_123");

        purchasePaymentService.completeSuccessfulPurchase(
                purchase.getId(), "cs_test_123", "pi_test_123"
        );

        ProgramAccess existingAccess = programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow();

        assertThrows(
                PurchasePaymentConfirmationException.class,
                () -> purchasePaymentService.completeSuccessfulPurchase(
                        purchase.getId(), "cs_test_123", "pi_test_other"
                )
        );

        Purchase persisted = purchaseRepository.findById(purchase.getId())
                .orElseThrow();
        ProgramAccess persistedAccess = programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow();

        assertEquals(PurchaseStatus.PAID, persisted.getStatus());
        assertEquals(NOW, persisted.getPaidAt());
        assertEquals("pi_test_123", persisted.getStripePaymentIntentId());
        assertEquals(existingAccess.getId(), persistedAccess.getId());
        assertEquals(1L, programAccessRepository.count());
    }

    @Test
    void shouldGrantMissingAccessForAlreadyPaidPurchase() {
        User user = createUser("member@example.com");
        Program program = createProgram("Six Week Strength");

        Purchase purchase = createPendingPurchase(
                user,
                program,
                "cs_test_123"
        );

        Instant originalPaidAt =
                Instant.parse("2026-09-15T12:00:00Z");

        purchase.markPaid(
                originalPaidAt,
                "pi_test_123"
        );

        purchaseRepository.saveAndFlush(purchase);

        assertTrue(
                programAccessRepository
                        .findByPurchaseId(purchase.getId())
                        .isEmpty()
        );

        purchasePaymentService.completeSuccessfulPurchase(
                purchase.getId(),
                "cs_test_123",
                "pi_test_123"
        );

        Purchase persisted = purchaseRepository
                .findById(purchase.getId())
                .orElseThrow();

        ProgramAccess access = programAccessRepository
                .findByPurchaseId(purchase.getId())
                .orElseThrow();

        assertEquals(PurchaseStatus.PAID, persisted.getStatus());
        assertEquals(originalPaidAt, persisted.getPaidAt());
        assertEquals("pi_test_123", persisted.getStripePaymentIntentId());

        assertEquals(originalPaidAt, access.getStartsAt());

        assertEquals(
                Instant.parse("2027-03-15T12:00:00Z"),
                access.getExpiresAt()
        );

        assertEquals(1L, programAccessRepository.count());
    }

    @Test
    void shouldGrantSeparateAccessesForDifferentPurchases() {
        User user = createUser("member@example.com");
        Program program = createProgram("Six Week Strength");

        Purchase firstPurchase = createPendingPurchase(
                user,
                program,
                "cs_test_first"
        );

        Purchase secondPurchase = createPendingPurchase(
                user,
                program,
                "cs_test_second"
        );

        purchasePaymentService.completeSuccessfulPurchase(
                firstPurchase.getId(),
                "cs_test_first",
                "pi_test_first"
        );

        purchasePaymentService.completeSuccessfulPurchase(
                secondPurchase.getId(),
                "cs_test_second",
                "pi_test_second"
        );

        ProgramAccess firstAccess = programAccessRepository
                .findByPurchaseId(firstPurchase.getId())
                .orElseThrow();

        ProgramAccess secondAccess = programAccessRepository
                .findByPurchaseId(secondPurchase.getId())
                .orElseThrow();

        assertNotEquals(firstAccess.getId(), secondAccess.getId());

        assertEquals(
                firstPurchase.getId(),
                firstAccess.getPurchase().getId()
        );

        assertEquals(
                secondPurchase.getId(),
                secondAccess.getPurchase().getId()
        );

        assertEquals(user.getId(), firstAccess.getUser().getId());
        assertEquals(user.getId(), secondAccess.getUser().getId());

        assertEquals(program.getId(), firstAccess.getProgram().getId());
        assertEquals(program.getId(), secondAccess.getProgram().getId());

        assertEquals(2L, programAccessRepository.count());
    }

    private Purchase createPendingPurchase(
            User user,
            Program program,
            String stripeCheckoutSessionId
    ) {
        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        8900L,
                        "USD"
                );

        purchase.attachStripeCheckoutSession(
                stripeCheckoutSessionId
        );

        return purchaseRepository.saveAndFlush(
                purchase
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