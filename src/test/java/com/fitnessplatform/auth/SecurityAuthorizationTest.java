package com.fitnessplatform.auth;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

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
    void shouldNotAuthenticateUsingHttpSession() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(
                        get("/api/auth/me")
                                .session(session)
                )
                .andExpect(status().isUnauthorized());
    }
}