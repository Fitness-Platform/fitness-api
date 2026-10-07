package com.fitnessplatform.purchase;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.program.ProgramStatus;
import com.fitnessplatform.purchase.stripe.StripeCheckoutGateway;
import com.fitnessplatform.purchase.stripe.StripeCheckoutSession;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import jakarta.persistence.EntityManager;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        TestcontainersConfiguration.class,
        PurchaseCheckoutControllerTest.StripeTestConfiguration.class
})
class PurchaseCheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository
            passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RecordingStripeCheckoutGateway
            stripeCheckoutGateway;

    @Autowired
    private EntityManager entityManager;


    @BeforeEach
    void setUp() {
        purchaseRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        programRepository.deleteAll();

        stripeCheckoutGateway.reset();
    }

    @AfterEach
    void cleanPurchases() {
        purchaseRepository.deleteAll();
    }

    @Test
    void shouldCreateCheckoutUsingServerControlledProgramPricing()
            throws Exception {

        User user =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Program program =
                new Program(
                        "Six Week Strength",
                        "Strength program"
                );

        program.updatePrice(
                4999L
        );

        program.publish();

        program =
                programRepository.saveAndFlush(
                        program
                );

        UUID programId =
                program.getId();

        entityManager.clear();

        Program persistedProgram =
                programRepository
                        .findById(
                                programId
                        )
                        .orElseThrow();

        assertEquals(
                ProgramStatus.PUBLISHED,
                persistedProgram.getStatus()
        );

        assertEquals(
                4999L,
                persistedProgram.getPriceCents()
        );

        Cookie authCookie =
                authenticatedCookie(
                        user
                );

        mockMvc.perform(
                        post(
                                "/api/programs/{programId}/checkout",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(authCookie)
                )
                .andExpect(
                        status().isCreated()
                );

        assertEquals(
                1,
                purchaseRepository.count()
        );

        Purchase purchase =
                purchaseRepository
                        .findAll()
                        .getFirst();

        assertEquals(
                user.getId(),
                purchase.getUser().getId()
        );

        assertEquals(
                program.getId(),
                purchase.getProgram().getId()
        );

        assertEquals(
                PurchaseStatus.PENDING,
                purchase.getStatus()
        );

        assertEquals(
                4999L,
                purchase.getAmountCents()
        );

        assertEquals(
                "USD",
                purchase.getCurrency()
        );

        assertNull(
                purchase.getPaidAt()
        );

        assertEquals(
                "cs_test_checkout_123",
                purchase.getStripeCheckoutSessionId()
        );

        assertEquals(
                purchase.getId(),
                stripeCheckoutGateway.purchaseId
        );

        assertEquals(
                "Six Week Strength",
                stripeCheckoutGateway.programName
        );

        assertEquals(
                4999L,
                stripeCheckoutGateway.amountCents
        );

        assertEquals(
                "USD",
                stripeCheckoutGateway.currency
        );
    }

    @Test
    void shouldRejectUnauthenticatedCheckout()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/programs/{programId}/checkout",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isUnauthorized()
                );

        assertEquals(
                0,
                purchaseRepository.count()
        );

        assertEquals(
                0,
                stripeCheckoutGateway.invocationCount
        );
    }

    @Test
    void shouldRejectCheckoutWithoutCsrf()
            throws Exception {

        User user =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Cookie authCookie =
                authenticatedCookie(
                        user
                );

        mockMvc.perform(
                        post(
                                "/api/programs/{programId}/checkout",
                                UUID.randomUUID()
                        )
                                .cookie(authCookie)
                )
                .andExpect(
                        status().isForbidden()
                );

        assertEquals(
                0,
                purchaseRepository.count()
        );

        assertEquals(
                0,
                stripeCheckoutGateway.invocationCount
        );
    }

    @Test
    void shouldRejectCheckoutForDraftProgram()
            throws Exception {

        User user =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Program program =
                new Program(
                        "Draft Program",
                        null
                );

        program.updatePrice(
                4999L
        );

        program =
                programRepository.saveAndFlush(
                        program
                );

        Cookie authCookie =
                authenticatedCookie(
                        user
                );

        mockMvc.perform(
                        post(
                                "/api/programs/{programId}/checkout",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(authCookie)
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program not purchasable"
                                )
                );

        assertEquals(
                0,
                purchaseRepository.count()
        );

        assertEquals(
                0,
                stripeCheckoutGateway.invocationCount
        );
    }

    @Test
    void shouldRejectCheckoutWhenProgramPriceIsNotConfigured()
            throws Exception {

        User user =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Program program =
                new Program(
                        "Unpriced Program",
                        null
                );

        program.publish();

        program =
                programRepository.saveAndFlush(
                        program
                );

        Cookie authCookie =
                authenticatedCookie(
                        user
                );

        mockMvc.perform(
                        post(
                                "/api/programs/{programId}/checkout",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(authCookie)
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program not purchasable"
                                )
                );

        assertEquals(
                0,
                purchaseRepository.count()
        );

        assertEquals(
                0,
                stripeCheckoutGateway.invocationCount
        );
    }

    @Test
    void shouldReturnNotFoundWhenCheckoutProgramDoesNotExist()
            throws Exception {

        User user =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Cookie authCookie =
                authenticatedCookie(
                        user
                );

        mockMvc.perform(
                        post(
                                "/api/programs/{programId}/checkout",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(authCookie)
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program not found"
                                )
                );

        assertEquals(
                0,
                purchaseRepository.count()
        );

        assertEquals(
                0,
                stripeCheckoutGateway.invocationCount
        );
    }

    private User createUser(
            String email,
            UserRole role
    ) {
        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(
                                "StrongPassword123!"
                        ),
                        role
                )
        );
    }

    private Cookie authenticatedCookie(
            User user
    ) {
        String token =
                jwtService.generateToken(
                        user.getId()
                );

        return new Cookie(
                "AUTH_TOKEN",
                token
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StripeTestConfiguration {

        @Bean
        @Primary
        RecordingStripeCheckoutGateway
        recordingStripeCheckoutGateway() {
            return new RecordingStripeCheckoutGateway();
        }
    }

    static class RecordingStripeCheckoutGateway
            implements StripeCheckoutGateway {

        private UUID purchaseId;
        private String programName;
        private Long amountCents;
        private String currency;
        private int invocationCount;

        @Override
        public StripeCheckoutSession createSession(
                UUID purchaseId,
                String programName,
                Long amountCents,
                String currency
        ) {
            invocationCount++;

            this.purchaseId = purchaseId;
            this.programName = programName;
            this.amountCents = amountCents;
            this.currency = currency;

            return new StripeCheckoutSession(
                    "cs_test_checkout_123",
                    "https://checkout.stripe.test/session"
            );
        }

        void reset() {
            purchaseId = null;
            programName = null;
            amountCents = null;
            currency = null;
            invocationCount = 0;
        }
    }
}