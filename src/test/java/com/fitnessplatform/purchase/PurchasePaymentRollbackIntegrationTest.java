package com.fitnessplatform.purchase;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccessRepository;
import com.fitnessplatform.access.PurchasedProgramAccessService;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PurchasePaymentRollbackIntegrationTest {

    @Autowired
    private PurchasePaymentService purchasePaymentService;

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

    @MockitoBean
    private PurchasedProgramAccessService purchasedProgramAccessService;

    @BeforeEach
    void cleanDatabase() {
        programAccessRepository.deleteAll();
        purchaseRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        programRepository.deleteAll();
    }

    @Test
    void shouldRollbackPurchaseWhenAccessGrantFails() {
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

        Purchase purchase = new Purchase(
                user,
                program,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession("cs_test_123");

        purchase = purchaseRepository.saveAndFlush(purchase);

        var purchaseId = purchase.getId();

        doThrow(new IllegalStateException("Simulated access failure"))
                .when(purchasedProgramAccessService)
                .grantForPurchase(any(Purchase.class));

        assertThrows(
                IllegalStateException.class,
                () -> purchasePaymentService.completeSuccessfulPurchase(
                        purchaseId,
                        "cs_test_123",
                        "pi_test_123"
                )
        );

        Purchase persisted = purchaseRepository
                .findById(purchaseId)
                .orElseThrow();

        assertEquals(
                PurchaseStatus.PENDING,
                persisted.getStatus()
        );

        assertNull(persisted.getPaidAt());

        assertNull(persisted.getStripePaymentIntentId());

        assertTrue(
                programAccessRepository
                        .findByPurchaseId(purchaseId)
                        .isEmpty()
        );
    }
}