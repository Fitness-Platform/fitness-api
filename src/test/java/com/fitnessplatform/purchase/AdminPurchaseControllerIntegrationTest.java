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
class AdminPurchaseControllerIntegrationTest {

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
    void shouldAllowAdminToListPurchasesFromDifferentMembers()
            throws Exception {

        User admin = createUser("admin@example.com", UserRole.ADMIN);
        User firstMember = createUser("first@example.com", UserRole.USER);
        User secondMember = createUser("second@example.com", UserRole.USER);

        Program program = createProgram("Strength Program");

        Purchase firstPurchase =
                createPurchase(firstMember, program, 8900L);

        Purchase secondPurchase =
                createPurchase(secondMember, program, 9900L);

        mockMvc.perform(
                        get("/api/admin/purchases")
                                .cookie(authenticatedCookie(admin))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(
                        jsonPath("$[?(@.purchaseId == '" +
                                firstPurchase.getId() + "')]").exists()
                )
                .andExpect(
                        jsonPath("$[?(@.purchaseId == '" +
                                secondPurchase.getId() + "')]").exists()
                );
    }

    @Test
    void shouldAllowAdminToRetrievePurchaseDetails()
            throws Exception {

        User admin = createUser("admin@example.com", UserRole.ADMIN);
        User buyer = createUser("buyer@example.com", UserRole.USER);
        Program program = createProgram("Strength Program");

        Purchase purchase = new Purchase(
                buyer,
                program,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession("cs_test_admin");

        purchase.markPaid(
                Instant.parse("2026-10-13T12:00:00Z"),
                "pi_test_admin"
        );

        purchase = purchaseRepository.saveAndFlush(purchase);

        mockMvc.perform(
                        get(
                                "/api/admin/purchases/{purchaseId}",
                                purchase.getId()
                        ).cookie(authenticatedCookie(admin))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.purchaseId")
                                .value(purchase.getId().toString())
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(buyer.getId().toString())
                )
                .andExpect(
                        jsonPath("$.programId")
                                .value(program.getId().toString())
                )
                .andExpect(
                        jsonPath("$.programName")
                                .value("Strength Program")
                )
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.amountCents").value(8900))
                .andExpect(jsonPath("$.currency").value("USD"))
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
    void shouldReturnEmptyListWhenNoPurchasesExist()
            throws Exception {

        User admin = createUser("admin@example.com", UserRole.ADMIN);

        mockMvc.perform(
                        get("/api/admin/purchases")
                                .cookie(authenticatedCookie(admin))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnNotFoundWhenAdminPurchaseDoesNotExist()
            throws Exception {

        User admin = createUser("admin@example.com", UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/purchases/{purchaseId}",
                                UUID.randomUUID()
                        ).cookie(authenticatedCookie(admin))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Purchase not found")
                );
    }

    @Test
    void shouldRejectRegularUserFromListingPurchases()
            throws Exception {

        User member = createUser("member@example.com", UserRole.USER);

        mockMvc.perform(
                        get("/api/admin/purchases")
                                .cookie(authenticatedCookie(member))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectRegularUserFromRetrievingPurchase()
            throws Exception {

        User member = createUser("member@example.com", UserRole.USER);

        mockMvc.perform(
                        get(
                                "/api/admin/purchases/{purchaseId}",
                                UUID.randomUUID()
                        ).cookie(authenticatedCookie(member))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedAdminPurchaseList()
            throws Exception {

        mockMvc.perform(get("/api/admin/purchases"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedAdminPurchaseDetails()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/admin/purchases/{purchaseId}",
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidJwtOnAdminPurchaseEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/purchases")
                                .cookie(new Cookie(
                                        "AUTH_TOKEN",
                                        "invalid-token"
                                ))
                )
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String email, UserRole role) {
        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode("StrongPassword123!"),
                        role
                )
        );
    }

    private Program createProgram(String name) {
        return programRepository.saveAndFlush(
                new Program(name, null)
        );
    }

    private Purchase createPurchase(
            User buyer,
            Program program,
            Long amountCents
    ) {
        return purchaseRepository.saveAndFlush(
                new Purchase(buyer, program, amountCents, "USD")
        );
    }

    private Cookie authenticatedCookie(User user) {
        String token = jwtService.generateToken(user.getId());

        return new Cookie("AUTH_TOKEN", token);
    }
}