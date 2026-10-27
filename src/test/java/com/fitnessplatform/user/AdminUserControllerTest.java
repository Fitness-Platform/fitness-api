package com.fitnessplatform.user;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccessRepository;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.purchase.PurchaseRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminUserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProgramAccessRepository programAccessRepository;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private JwtService jwtService;

    private User admin;

    @BeforeEach
    void setUp() {
        programAccessRepository.deleteAll();
        purchaseRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("admin@platform.test", UserRole.ADMIN, "2026-01-01T00:00:00Z");
    }

    @Test
    void shouldListUsersNewestFirstAsAdminWithOnlyPublicFields() throws Exception {
        User member = createUser("member@example.com", UserRole.USER, "2026-02-01T00:00:00Z");

        mockMvc.perform(get("/api/admin/users").cookie(cookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]", aMapWithSize(4)))
                .andExpect(jsonPath("$[0].id").value(member.getId().toString()))
                .andExpect(jsonPath("$[0].email").value("member@example.com"))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-02-01T00:00:00Z"))
                .andExpect(jsonPath("$[1]", aMapWithSize(4)))
                .andExpect(jsonPath("$[1].id").value(admin.getId().toString()))
                .andExpect(jsonPath("$[1].role").value("ADMIN"))
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist());
    }

    @Test
    void shouldSearchByEmailSubstringIgnoringCaseAndWhitespace() throws Exception {
        createUser("First@Example.com", UserRole.USER, "2026-02-01T00:00:00Z");
        createUser("second@example.COM", UserRole.USER, "2026-03-01T00:00:00Z");
        createUser("other@elsewhere.test", UserRole.USER, "2026-04-01T00:00:00Z");

        mockMvc.perform(get("/api/admin/users").param("email", "  EXAMPLE  ").cookie(cookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("second@example.COM"))
                .andExpect(jsonPath("$[1].email").value("First@Example.com"))
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist());
    }

    @Test
    void shouldListUsersForBlankSearch() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("email", "  ").cookie(cookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnEmptyListWhenNoEmailMatches() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("email", "no-match").cookie(cookie(admin)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldRetrieveUserWithOnlyPublicFields() throws Exception {
        User member = createUser("member@example.com", UserRole.USER, "2026-02-01T00:00:00Z");

        mockMvc.perform(get("/api/admin/users/{userId}", member.getId()).cookie(cookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.id").value(member.getId().toString()))
                .andExpect(jsonPath("$.email").value("member@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").value("2026-02-01T00:00:00Z"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturnExistingNotFoundProblemForUnknownUser() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/admin/users/{userId}", unknownId).cookie(cookie(admin)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.detail").value("User not found: " + unknownId));
    }

    @Test
    void shouldRejectMalformedUserId() throws Exception {
        mockMvc.perform(get("/api/admin/users/not-a-uuid").cookie(cookie(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNonAdminOnBothEndpoints() throws Exception {
        User member = createUser("member@example.com", UserRole.USER, "2026-02-01T00:00:00Z");

        mockMvc.perform(get("/api/admin/users").cookie(cookie(member)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users/{userId}", admin.getId()).cookie(cookie(member)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAnonymousOnBothEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users/{userId}", admin.getId()))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String email, UserRole role, String createdAt) {
        User user = new User(email, "never-expose-this-password-hash", role);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse(createdAt));
        return userRepository.saveAndFlush(user);
    }

    private Cookie cookie(User user) {
        return new Cookie("AUTH_TOKEN", jwtService.generateToken(user.getId()));
    }
}
