package com.fitnessplatform.auth;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRejectProtectedEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        get("/api/auth/me")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowProtectedEndpointWithValidAuthenticationCookie()
            throws Exception {

        User user = new User(
                "samuel@example.com",
                passwordEncoder.encode("StrongPassword123!"),
                UserRole.USER
        );

        user = userRepository.saveAndFlush(user);

        String token = jwtService.generateToken(user.getId());

        mockMvc.perform(
                        get("/api/auth/me")
                                .cookie(
                                        new Cookie(
                                                "AUTH_TOKEN",
                                                token
                                        )
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectProtectedEndpointWithInvalidAuthenticationToken()
            throws Exception {

        mockMvc.perform(
                        get("/api/auth/me")
                                .cookie(
                                        new Cookie(
                                                "AUTH_TOKEN",
                                                "invalid-token"
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectTokenWhenUserNoLongerExists() throws Exception {
        User user = new User(
                "deleted@example.com",
                passwordEncoder.encode("StrongPassword123!"),
                UserRole.USER
        );

        user = userRepository.saveAndFlush(user);

        String token = jwtService.generateToken(user.getId());

        userRepository.delete(user);
        userRepository.flush();

        mockMvc.perform(
                        get("/api/auth/me")
                                .cookie(
                                        new Cookie(
                                                "AUTH_TOKEN",
                                                token
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowCsrfEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(
                        get("/api/auth/csrf")
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectPublicLoginEndpointWithoutCsrf() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "user@example.com",
                                      "password": "StrongPassword123!"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectPublicRegistrationEndpointWithoutCsrf() throws Exception {
        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "user@example.com",
                                      "password": "StrongPassword123!"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectPublicLogoutEndpointWithoutCsrf() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldIgnoreAuthenticationStoredInHttpSession()
            throws Exception {

        UUID userId = UUID.randomUUID();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of()
                );

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        MockHttpSession session = new MockHttpSession();

        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(session)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectProtectedEndpointWithExpiredAuthenticationToken()
            throws Exception {

        User user = new User(
                "expired@example.com",
                passwordEncoder.encode("StrongPassword123!"),
                UserRole.USER
        );

        user = userRepository.saveAndFlush(user);

        JwtService expiredJwtService = new JwtService(
                jwtSecret,
                Duration.ofSeconds(-1)
        );

        String expiredToken =
                expiredJwtService.generateToken(user.getId());

        mockMvc.perform(
                        get("/api/auth/me")
                                .cookie(
                                        new Cookie(
                                                "AUTH_TOKEN",
                                                expiredToken
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUnauthenticatedAccessToAdminRoutes() throws Exception {
        mockMvc.perform(
                        get("/api/admin/non-existing")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectUserAccessToAdminRoutes() throws Exception {
        User user = new User(
                "user@example.com",
                passwordEncoder.encode("StrongPassword123!"),
                UserRole.USER
        );

        user = userRepository.saveAndFlush(user);

        String token = jwtService.generateToken(user.getId());

        mockMvc.perform(
                        get("/api/admin/non-existing")
                                .cookie(
                                        new Cookie(
                                                "AUTH_TOKEN",
                                                token
                                        )
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminThroughAdminAuthorizationBoundary() throws Exception {
        User admin = new User(
                "admin@example.com",
                passwordEncoder.encode("StrongPassword123!"),
                UserRole.ADMIN
        );

        admin = userRepository.saveAndFlush(admin);

        String token = jwtService.generateToken(admin.getId());

        mockMvc.perform(
                        get("/api/admin/non-existing")
                                .cookie(
                                        new Cookie(
                                                "AUTH_TOKEN",
                                                token
                                        )
                                )
                )
                .andExpect(status().isNotFound());
    }
}