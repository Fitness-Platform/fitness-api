package com.fitnessplatform.purchase;

import com.fitnessplatform.TestcontainersConfiguration;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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