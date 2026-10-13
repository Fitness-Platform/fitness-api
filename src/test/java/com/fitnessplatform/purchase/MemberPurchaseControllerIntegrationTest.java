package com.fitnessplatform.purchase;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccessRepository;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MemberPurchaseControllerIntegrationTest {

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

    @BeforeEach
    void cleanDatabase() {
        programAccessRepository.deleteAll();
        purchaseRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        programRepository.deleteAll();
    }

    @AfterEach
    void cleanPurchases() {
        purchaseRepository.deleteAll();
    }

    @Test
    void shouldListOnlyAuthenticatedMembersPurchases() throws Exception {
        User member = createUser("member@example.com");
        User anotherMember = createUser("another@example.com");

        Program program = createProgram("Strength Program");

        Purchase ownPurchase = createPurchase(
                member, program, 8900L
        );

        createPurchase(anotherMember, program, 9900L);

        mockMvc.perform(
                        get("/api/me/purchases")
                                .cookie(authenticatedCookie(member))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(
                        jsonPath("$[0].purchaseId")
                                .value(ownPurchase.getId().toString())
                )
                .andExpect(
                        jsonPath("$[0].programId")
                                .value(program.getId().toString())
                )
                .andExpect(
                        jsonPath("$[0].programName")
                                .value("Strength Program")
                )
                .andExpect(
                        jsonPath("$[0].amountCents")
                                .value(8900)
                )
                .andExpect(
                        jsonPath("$[0].currency")
                                .value("USD")
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("PENDING")
                )
                .andExpect(
                        jsonPath("$[0].stripeCheckoutSessionId")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$[0].stripePaymentIntentId")
                                .doesNotExist()
                );
    }

    @Test
    void shouldReturnEmptyListWhenMemberHasNoPurchases() throws Exception {
        User member = createUser("member@example.com");

        mockMvc.perform(
                        get("/api/me/purchases")
                                .cookie(authenticatedCookie(member))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnOwnPurchaseDetails() throws Exception {
        User member = createUser("member@example.com");
        Program program = createProgram("Strength Program");

        Purchase purchase = new Purchase(
                member,
                program,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession("cs_test_member_purchase");

        Instant paidAt = Instant.parse("2026-10-13T12:00:00Z");

        purchase.markPaid(
                paidAt,
                "pi_test_member_purchase"
        );

        purchase = purchaseRepository.saveAndFlush(purchase);

        mockMvc.perform(
                        get(
                                "/api/me/purchases/{purchaseId}",
                                purchase.getId()
                        ).cookie(authenticatedCookie(member))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.purchaseId")
                                .value(purchase.getId().toString())
                )
                .andExpect(
                        jsonPath("$.programId")
                                .value(program.getId().toString())
                )
                .andExpect(
                        jsonPath("$.programName")
                                .value("Strength Program")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PAID")
                )
                .andExpect(
                        jsonPath("$.amountCents")
                                .value(8900)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                )
                .andExpect(
                        jsonPath("$.createdAt")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.paidAt")
                                .value("2026-10-13T12:00:00Z")
                )
                .andExpect(
                        jsonPath("$.stripeCheckoutSessionId")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.stripePaymentIntentId")
                                .doesNotExist()
                );
    }

    @Test
    void shouldReturnNotFoundForNonexistentPurchase() throws Exception {
        User member = createUser("member@example.com");

        mockMvc.perform(
                        get(
                                "/api/me/purchases/{purchaseId}",
                                UUID.randomUUID()
                        ).cookie(authenticatedCookie(member))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Purchase not found")
                );
    }

    @Test
    void shouldReturnNotFoundForAnotherMembersPurchase() throws Exception {
        User owner = createUser("owner@example.com");
        User anotherMember = createUser("another@example.com");

        Program program = createProgram("Strength Program");

        Purchase purchase = createPurchase(
                owner,
                program,
                8900L
        );

        mockMvc.perform(
                        get(
                                "/api/me/purchases/{purchaseId}",
                                purchase.getId()
                        ).cookie(authenticatedCookie(anotherMember))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Purchase not found")
                );
    }

    @Test
    void shouldRejectUnauthenticatedPurchaseDetailRequest()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/me/purchases/{purchaseId}",
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedPurchaseListRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/me/purchases")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnExpiredPurchaseStatus() throws Exception {
        User member = createUser("member@example.com");
        Program program = createProgram("Strength Program");

        Purchase purchase = new Purchase(
                member,
                program,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession("cs_test_expired_member");
        purchase.markExpired();

        purchase = purchaseRepository.saveAndFlush(purchase);

        mockMvc.perform(
                        get(
                                "/api/me/purchases/{purchaseId}",
                                purchase.getId()
                        ).cookie(authenticatedCookie(member))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.amountCents").value(8900))
                .andExpect(jsonPath("$.paidAt").value(
                        org.hamcrest.Matchers.nullValue()
                ))
                .andExpect(
                        jsonPath("$.stripeCheckoutSessionId").doesNotExist()
                )
                .andExpect(
                        jsonPath("$.stripePaymentIntentId").doesNotExist()
                );
    }

    @Test
    void shouldRejectPurchaseRequestWithInvalidJwt() throws Exception {
        mockMvc.perform(
                        get("/api/me/purchases")
                                .cookie(
                                        new Cookie(
                                                "AUTH_TOKEN",
                                                "invalid-token"
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String email) {
        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode("StrongPassword123!"),
                        UserRole.USER
                )
        );
    }

    private Program createProgram(String name) {
        return programRepository.saveAndFlush(
                new Program(name, null)
        );
    }

    private Purchase createPurchase(
            User user,
            Program program,
            Long amountCents
    ) {
        return purchaseRepository.saveAndFlush(
                new Purchase(
                        user,
                        program,
                        amountCents,
                        "USD"
                )
        );
    }

    private Cookie authenticatedCookie(User user) {
        String token = jwtService.generateToken(user.getId());

        return new Cookie(
                "AUTH_TOKEN",
                token
        );
    }
}