package com.fitnessplatform.purchase;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PurchaseIntegrationTest {

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

    @AfterEach
    void cleanPurchasesAfterTest() {
        purchaseRepository.deleteAll();
    }

    @Test
    void shouldPersistPurchase() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                purchaseRepository.saveAndFlush(
                        new Purchase(
                                user,
                                program,
                                4999L,
                                "USD"
                        )
                );

        Purchase persisted =
                purchaseRepository
                        .findById(
                                purchase.getId()
                        )
                        .orElseThrow();

        assertEquals(
                user.getId(),
                persisted.getUser().getId()
        );

        assertEquals(
                program.getId(),
                persisted.getProgram().getId()
        );

        assertEquals(
                4999L,
                persisted.getAmountCents()
        );

        assertEquals(
                "USD",
                persisted.getCurrency()
        );

        assertEquals(
                PurchaseStatus.PENDING,
                persisted.getStatus()
        );

        assertNull(
                persisted.getPaidAt()
        );

        assertNull(
                persisted.getStripeCheckoutSessionId()
        );

        assertNotNull(
                persisted.getCreatedAt()
        );

        assertNotNull(
                persisted.getUpdatedAt()
        );
    }

    @Test
    void shouldPersistStripeCheckoutSessionId() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        4999L,
                        "USD"
                );

        purchase.attachStripeCheckoutSession(
                "cs_test_example"
        );

        purchase =
                purchaseRepository.saveAndFlush(
                        purchase
                );

        Purchase persisted =
                purchaseRepository
                        .findById(
                                purchase.getId()
                        )
                        .orElseThrow();

        assertEquals(
                "cs_test_example",
                persisted.getStripeCheckoutSessionId()
        );
    }

    @Test
    void shouldAllowMultiplePurchasesForSameUserAndProgram() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase firstPurchase =
                purchaseRepository.saveAndFlush(
                        new Purchase(
                                user,
                                program,
                                4999L,
                                "USD"
                        )
                );

        Purchase secondPurchase =
                purchaseRepository.saveAndFlush(
                        new Purchase(
                                user,
                                program,
                                5999L,
                                "USD"
                        )
                );

        assertEquals(
                2,
                purchaseRepository.count()
        );

        assertTrue(
                purchaseRepository.existsById(
                        firstPurchase.getId()
                )
        );

        assertTrue(
                purchaseRepository.existsById(
                        secondPurchase.getId()
                )
        );
    }


    @Test
    void shouldRejectPurchaseWithNonPositiveAmount() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        0L,
                        "USD"
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        purchaseRepository.saveAndFlush(
                                purchase
                        )
        );
    }

    @Test
    void shouldRejectPurchaseWithInvalidCurrencyLength() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        4999L,
                        "US"
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        purchaseRepository.saveAndFlush(
                                purchase
                        )
        );
    }

    @Test
    void shouldPreventDeletingUserReferencedByPurchase() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        purchaseRepository.saveAndFlush(
                new Purchase(
                        user,
                        program,
                        4999L,
                        "USD"
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    userRepository.delete(
                            user
                    );

                    userRepository.flush();
                }
        );

        assertTrue(
                userRepository.existsById(
                        user.getId()
                )
        );
    }

    @Test
    void shouldPreventDeletingProgramReferencedByPurchase() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        purchaseRepository.saveAndFlush(
                new Purchase(
                        user,
                        program,
                        4999L,
                        "USD"
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    programRepository.delete(
                            program
                    );

                    programRepository.flush();
                }
        );

        assertTrue(
                programRepository.existsById(
                        program.getId()
                )
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