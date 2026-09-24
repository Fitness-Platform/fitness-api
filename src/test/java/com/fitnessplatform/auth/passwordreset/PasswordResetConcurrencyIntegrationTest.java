package com.fitnessplatform.auth.passwordreset;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PasswordResetConcurrencyIntegrationTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldAllowPasswordResetTokenToBeConsumedOnlyOnceUnderConcurrency()
            throws Exception {

        User user = new User(
                "samuel@example.com",
                passwordEncoder.encode("OldPassword123!"),
                UserRole.USER
        );

        userRepository.saveAndFlush(user);

        String rawToken =
                passwordResetService.requestReset(
                        "samuel@example.com"
                );

        assertNotNull(rawToken);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Callable<AttemptResult> firstAttempt = () ->
                    attemptReset(
                            rawToken,
                            "FirstNewPassword123!",
                            ready,
                            start
                    );

            Callable<AttemptResult> secondAttempt = () ->
                    attemptReset(
                            rawToken,
                            "SecondNewPassword123!",
                            ready,
                            start
                    );

            Future<AttemptResult> firstFuture =
                    executor.submit(firstAttempt);

            Future<AttemptResult> secondFuture =
                    executor.submit(secondAttempt);

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS)
            );

            start.countDown();

            AttemptResult firstResult =
                    firstFuture.get(10, TimeUnit.SECONDS);

            AttemptResult secondResult =
                    secondFuture.get(10, TimeUnit.SECONDS);

            long successCount = Stream
                    .of(firstResult, secondResult)
                    .filter(result ->
                            result == AttemptResult.SUCCESS
                    )
                    .count();

            long rejectedCount = Stream
                    .of(firstResult, secondResult)
                    .filter(result ->
                            result == AttemptResult.REJECTED
                    )
                    .count();

            assertEquals(1, successCount);
            assertEquals(1, rejectedCount);

            assertEquals(0, tokenRepository.count());

            User updatedUser = userRepository
                    .findByEmail("samuel@example.com")
                    .orElseThrow();

            assertFalse(
                    passwordEncoder.matches(
                            "OldPassword123!",
                            updatedUser.getPasswordHash()
                    )
            );

            boolean firstPasswordWon =
                    passwordEncoder.matches(
                            "FirstNewPassword123!",
                            updatedUser.getPasswordHash()
                    );

            boolean secondPasswordWon =
                    passwordEncoder.matches(
                            "SecondNewPassword123!",
                            updatedUser.getPasswordHash()
                    );

            assertTrue(
                    firstPasswordWon || secondPasswordWon
            );

        } finally {
            executor.shutdownNow();
        }
    }

    private AttemptResult attemptReset(
            String rawToken,
            String newPassword,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {

        ready.countDown();
        start.await();

        try {
            passwordResetService.resetPassword(
                    rawToken,
                    newPassword
            );

            return AttemptResult.SUCCESS;

        } catch (InvalidPasswordResetTokenException exception) {
            return AttemptResult.REJECTED;
        }
    }

    private enum AttemptResult {
        SUCCESS,
        REJECTED
    }
}