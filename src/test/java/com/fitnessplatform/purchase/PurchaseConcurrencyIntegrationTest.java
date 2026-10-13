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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import({
        TestcontainersConfiguration.class,
        PurchaseConcurrencyIntegrationTest.FixedClockConfiguration.class
})
class PurchaseConcurrencyIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    @Autowired private PurchasePaymentService paymentService;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private ProgramAccessRepository programAccessRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProgramRepository programRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private PurchaseExpirationService expirationService;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void cleanDatabase() {
        programAccessRepository.deleteAll();
        purchaseRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        programRepository.deleteAll();
    }

    @Test
    void shouldSerializeConcurrentConfirmationsForSamePurchase() throws Exception {
        User buyer = userRepository.saveAndFlush(new User(
                "concurrent@example.com",
                passwordEncoder.encode("StrongPassword123!"),
                UserRole.USER
        ));
        Program program = programRepository.saveAndFlush(new Program("Strength Program", null));
        Purchase purchase = new Purchase(buyer, program, 8900L, "USD");
        purchase.attachStripeCheckoutSession("cs_test_concurrent");
        purchase = purchaseRepository.saveAndFlush(purchase);
        UUID purchaseId = purchase.getId();

        CountDownLatch firstConfirmedButNotCommitted = new CountDownLatch(1);
        CountDownLatch allowFirstCommit = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> {
                transactionTemplate.executeWithoutResult(status -> {
                    paymentService.completeSuccessfulPurchase(
                            purchaseId, "cs_test_concurrent", "pi_test_concurrent");
                    firstConfirmedButNotCommitted.countDown();
                    try {
                        if (!allowFirstCommit.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out waiting to commit first transaction");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                });
            });

            assertTrue(firstConfirmedButNotCommitted.await(10, TimeUnit.SECONDS),
                    "First transaction did not reach the hold point");

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                paymentService.completeSuccessfulPurchase(
                        purchaseId, "cs_test_concurrent", "pi_test_concurrent");
            });

            assertTrue(secondStarted.await(10, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
                    () -> second.get(300, TimeUnit.MILLISECONDS),
                    "Second confirmation should wait while the first transaction holds the row lock");

            allowFirstCommit.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            allowFirstCommit.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        Purchase persisted = purchaseRepository.findById(purchaseId).orElseThrow();
        ProgramAccess access = programAccessRepository.findByPurchaseId(purchaseId).orElseThrow();

        assertEquals(PurchaseStatus.PAID, persisted.getStatus());
        assertEquals(NOW, persisted.getPaidAt());
        assertEquals("pi_test_concurrent", persisted.getStripePaymentIntentId());
        assertEquals(purchaseId, access.getPurchase().getId());
        assertEquals(NOW, access.getStartsAt());
        assertEquals(Instant.parse("2027-04-09T12:00:00Z"), access.getExpiresAt());
        assertEquals(1L, programAccessRepository.count());
    }

    @Test
    void shouldRejectConcurrentConfirmationWithDifferentPaymentIntent()
            throws Exception {

        User buyer = userRepository.saveAndFlush(
                new User(
                        "conflict@example.com",
                        passwordEncoder.encode("StrongPassword123!"),
                        UserRole.USER
                )
        );

        Program program = programRepository.saveAndFlush(
                new Program("Strength Program", null)
        );

        Purchase purchase = new Purchase(
                buyer,
                program,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession("cs_test_conflict");

        purchase = purchaseRepository.saveAndFlush(purchase);

        UUID purchaseId = purchase.getId();

        CountDownLatch firstConfirmedButNotCommitted =
                new CountDownLatch(1);

        CountDownLatch allowFirstCommit =
                new CountDownLatch(1);

        CountDownLatch secondStarted =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> {
                transactionTemplate.executeWithoutResult(status -> {
                    paymentService.completeSuccessfulPurchase(
                            purchaseId,
                            "cs_test_conflict",
                            "pi_original"
                    );

                    firstConfirmedButNotCommitted.countDown();

                    try {
                        if (!allowFirstCommit.await(
                                10,
                                TimeUnit.SECONDS
                        )) {
                            throw new IllegalStateException(
                                    "Timed out waiting to commit first transaction"
                            );
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                });
            });

            assertTrue(
                    firstConfirmedButNotCommitted.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "First transaction did not reach the hold point"
            );

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();

                paymentService.completeSuccessfulPurchase(
                        purchaseId,
                        "cs_test_conflict",
                        "pi_different"
                );
            });

            assertTrue(
                    secondStarted.await(10, TimeUnit.SECONDS)
            );

            assertThrows(
                    TimeoutException.class,
                    () -> second.get(300, TimeUnit.MILLISECONDS),
                    "Second confirmation should remain blocked before first commit"
            );

            allowFirstCommit.countDown();

            // The original payment must commit successfully.
            first.get(10, TimeUnit.SECONDS);

            // The conflicting confirmation must be rejected.
            java.util.concurrent.ExecutionException exception =
                    assertThrows(
                            java.util.concurrent.ExecutionException.class,
                            () -> second.get(10, TimeUnit.SECONDS)
                    );

            assertInstanceOf(
                    PurchasePaymentConfirmationException.class,
                    exception.getCause()
            );

        } finally {
            allowFirstCommit.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(10, TimeUnit.SECONDS)
            );
        }

        Purchase persisted = purchaseRepository
                .findById(purchaseId)
                .orElseThrow();

        ProgramAccess access = programAccessRepository
                .findByPurchaseId(purchaseId)
                .orElseThrow();

        assertEquals(
                PurchaseStatus.PAID,
                persisted.getStatus()
        );

        assertEquals(
                "pi_original",
                persisted.getStripePaymentIntentId()
        );

        assertEquals(
                NOW,
                persisted.getPaidAt()
        );

        assertEquals(
                purchaseId,
                access.getPurchase().getId()
        );

        assertEquals(
                NOW,
                access.getStartsAt()
        );

        assertEquals(
                Instant.parse("2027-04-09T12:00:00Z"),
                access.getExpiresAt()
        );

        assertEquals(
                1L,
                programAccessRepository.count()
        );
    }

    @Test
    void shouldPreservePaidPurchaseWhenExpirationArrivesConcurrently()
            throws Exception {

        User buyer = userRepository.saveAndFlush(
                new User(
                        "payment-wins@example.com",
                        passwordEncoder.encode("StrongPassword123!"),
                        UserRole.USER
                )
        );

        Program program = programRepository.saveAndFlush(
                new Program("Strength Program", null)
        );

        Purchase purchase = new Purchase(
                buyer,
                program,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession(
                "cs_test_payment_wins"
        );

        purchase = purchaseRepository.saveAndFlush(purchase);

        UUID purchaseId = purchase.getId();

        CountDownLatch paymentCompletedButNotCommitted =
                new CountDownLatch(1);

        CountDownLatch allowPaymentCommit =
                new CountDownLatch(1);

        CountDownLatch expirationStarted =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<?> payment = executor.submit(() -> {
                transactionTemplate.executeWithoutResult(status -> {
                    paymentService.completeSuccessfulPurchase(
                            purchaseId,
                            "cs_test_payment_wins",
                            "pi_test_payment_wins"
                    );

                    paymentCompletedButNotCommitted.countDown();

                    try {
                        if (!allowPaymentCommit.await(
                                10,
                                TimeUnit.SECONDS
                        )) {
                            throw new IllegalStateException(
                                    "Timed out waiting to commit payment"
                            );
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                });
            });

            assertTrue(
                    paymentCompletedButNotCommitted.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "Payment transaction did not reach hold point"
            );

            Future<?> expiration = executor.submit(() -> {
                expirationStarted.countDown();

                expirationService.expirePurchase(
                        purchaseId,
                        "cs_test_payment_wins"
                );
            });

            assertTrue(
                    expirationStarted.await(
                            10,
                            TimeUnit.SECONDS
                    )
            );

            assertThrows(
                    TimeoutException.class,
                    () -> expiration.get(
                            300,
                            TimeUnit.MILLISECONDS
                    ),
                    "Expiration should wait for payment transaction"
            );

            allowPaymentCommit.countDown();

            payment.get(10, TimeUnit.SECONDS);
            expiration.get(10, TimeUnit.SECONDS);

        } finally {
            allowPaymentCommit.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            10,
                            TimeUnit.SECONDS
                    )
            );
        }

        Purchase persisted = purchaseRepository
                .findById(purchaseId)
                .orElseThrow();

        ProgramAccess access = programAccessRepository
                .findByPurchaseId(purchaseId)
                .orElseThrow();

        assertEquals(
                PurchaseStatus.PAID,
                persisted.getStatus()
        );

        assertEquals(
                "pi_test_payment_wins",
                persisted.getStripePaymentIntentId()
        );

        assertEquals(
                NOW,
                persisted.getPaidAt()
        );

        assertEquals(
                purchaseId,
                access.getPurchase().getId()
        );

        assertEquals(
                NOW,
                access.getStartsAt()
        );

        assertEquals(
                Instant.parse("2027-04-09T12:00:00Z"),
                access.getExpiresAt()
        );

        assertEquals(
                1L,
                programAccessRepository.count()
        );
    }

    @Test
    void shouldSerializeConcurrentExpirationsForSamePurchase()
            throws Exception {

        User buyer = userRepository.saveAndFlush(
                new User(
                        "concurrent-expiration@example.com",
                        passwordEncoder.encode("StrongPassword123!"),
                        UserRole.USER
                )
        );

        Program program = programRepository.saveAndFlush(
                new Program("Strength Program", null)
        );

        Purchase purchase = new Purchase(
                buyer,
                program,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession(
                "cs_test_concurrent_expiration"
        );

        purchase = purchaseRepository.saveAndFlush(purchase);

        UUID purchaseId = purchase.getId();

        CountDownLatch firstExpiredButNotCommitted =
                new CountDownLatch(1);

        CountDownLatch allowFirstCommit =
                new CountDownLatch(1);

        CountDownLatch secondStarted =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> {
                transactionTemplate.executeWithoutResult(status -> {
                    expirationService.expirePurchase(
                            purchaseId,
                            "cs_test_concurrent_expiration"
                    );

                    firstExpiredButNotCommitted.countDown();

                    try {
                        if (!allowFirstCommit.await(
                                10,
                                TimeUnit.SECONDS
                        )) {
                            throw new IllegalStateException(
                                    "Timed out waiting to commit expiration"
                            );
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                });
            });

            assertTrue(
                    firstExpiredButNotCommitted.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "First expiration did not reach the hold point"
            );

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();

                expirationService.expirePurchase(
                        purchaseId,
                        "cs_test_concurrent_expiration"
                );
            });

            assertTrue(
                    secondStarted.await(
                            10,
                            TimeUnit.SECONDS
                    )
            );

            assertThrows(
                    TimeoutException.class,
                    () -> second.get(300, TimeUnit.MILLISECONDS),
                    "Second expiration should wait for the first transaction"
            );

            allowFirstCommit.countDown();

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);

        } finally {
            allowFirstCommit.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            10,
                            TimeUnit.SECONDS
                    )
            );
        }

        Purchase persisted = purchaseRepository
                .findById(purchaseId)
                .orElseThrow();

        assertEquals(
                PurchaseStatus.EXPIRED,
                persisted.getStatus()
        );

        assertEquals(
                "cs_test_concurrent_expiration",
                persisted.getStripeCheckoutSessionId()
        );

        assertNull(persisted.getPaidAt());
        assertNull(persisted.getStripePaymentIntentId());

        assertTrue(
                programAccessRepository
                        .findByPurchaseId(purchaseId)
                        .isEmpty()
        );

        assertEquals(0L, programAccessRepository.count());
    }

    @Test
    void shouldRejectPaymentWhenExpirationCommitsFirst()
            throws Exception {

        User buyer = userRepository.saveAndFlush(
                new User(
                        "expiration-wins@example.com",
                        passwordEncoder.encode("StrongPassword123!"),
                        UserRole.USER
                )
        );

        Program program = programRepository.saveAndFlush(
                new Program("Strength Program", null)
        );

        Purchase purchase = new Purchase(
                buyer,
                program,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession(
                "cs_test_expiration_wins"
        );

        purchase = purchaseRepository.saveAndFlush(purchase);

        UUID purchaseId = purchase.getId();

        CountDownLatch expirationCompletedButNotCommitted =
                new CountDownLatch(1);

        CountDownLatch allowExpirationCommit =
                new CountDownLatch(1);

        CountDownLatch paymentStarted =
                new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<?> expiration = executor.submit(() -> {
                transactionTemplate.executeWithoutResult(status -> {

                    expirationService.expirePurchase(
                            purchaseId,
                            "cs_test_expiration_wins"
                    );

                    expirationCompletedButNotCommitted.countDown();

                    try {
                        if (!allowExpirationCommit.await(
                                10,
                                TimeUnit.SECONDS
                        )) {
                            throw new IllegalStateException(
                                    "Timed out waiting to commit expiration"
                            );
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();

                        throw new IllegalStateException(
                                exception
                        );
                    }
                });
            });

            assertTrue(
                    expirationCompletedButNotCommitted.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "Expiration transaction did not reach hold point"
            );

            Future<?> payment = executor.submit(() -> {
                paymentStarted.countDown();

                paymentService.completeSuccessfulPurchase(
                        purchaseId,
                        "cs_test_expiration_wins",
                        "pi_test_expiration_wins"
                );
            });

            assertTrue(
                    paymentStarted.await(
                            10,
                            TimeUnit.SECONDS
                    )
            );

            assertThrows(
                    TimeoutException.class,
                    () -> payment.get(
                            300,
                            TimeUnit.MILLISECONDS
                    ),
                    "Payment should not complete while expiration holds the row lock"
            );

            allowExpirationCommit.countDown();

            expiration.get(10, TimeUnit.SECONDS);

            // The payment must be rejected after reading EXPIRED.
            java.util.concurrent.ExecutionException exception =
                    assertThrows(
                            java.util.concurrent.ExecutionException.class,
                            () -> payment.get(
                                    10,
                                    TimeUnit.SECONDS
                            )
                    );

            assertInstanceOf(
                    PurchasePaymentConfirmationException.class,
                    exception.getCause()
            );

        } finally {
            allowExpirationCommit.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            10,
                            TimeUnit.SECONDS
                    )
            );
        }

        Purchase persisted = purchaseRepository
                .findById(purchaseId)
                .orElseThrow();

        assertEquals(
                PurchaseStatus.EXPIRED,
                persisted.getStatus()
        );

        assertEquals(
                "cs_test_expiration_wins",
                persisted.getStripeCheckoutSessionId()
        );

        assertNull(persisted.getPaidAt());

        assertNull(persisted.getStripePaymentIntentId());

        assertTrue(
                programAccessRepository
                        .findByPurchaseId(purchaseId)
                        .isEmpty()
        );

        assertEquals(
                0L,
                programAccessRepository.count()
        );
    }
}
