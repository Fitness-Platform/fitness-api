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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

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
    void shouldRegisterUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Samuel@Example.com",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email")
                        .value("samuel@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User user = userRepository
                .findByEmail("samuel@example.com")
                .orElseThrow();

        assertFalse(
                user.getPasswordHash().equals("StrongPassword123!")
        );

        assertTrue(
                passwordEncoder.matches(
                        "StrongPassword123!",
                        user.getPasswordHash()
                )
        );
    }

    @Test
    void shouldLoginAndSetAuthenticationCookie() throws Exception {
        String rawPassword = "StrongPassword123!";

        User user = new User(
                "samuel@example.com",
                passwordEncoder.encode(rawPassword),
                UserRole.USER
        );

        user = userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "samuel@example.com",
                              "password": "StrongPassword123!"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("samuel@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("AUTH_TOKEN=")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("HttpOnly")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Path=/")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("SameSite=Lax")
                ));
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        User existingUser = new User(
                "existing@example.com",
                passwordEncoder.encode("ExistingPassword123!"),
                UserRole.USER
        );

        userRepository.saveAndFlush(existingUser);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "existing@example.com",
                                  "password": "StrongPassword123!"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Email already registered"));
    }

    @Test
    void shouldReturnUnauthorizedWhenEmailDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "unknown@example.com",
                              "password": "StrongPassword123!"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Invalid credentials"));
    }

    @Test
    void shouldRejectInvalidRegistrationInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWhenPasswordIsIncorrect() throws Exception {
        User user = new User(
                "samuel@example.com",
                passwordEncoder.encode("CorrectPassword123!"),
                UserRole.USER
        );

        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "samuel@example.com",
                              "password": "WrongPassword123!"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Invalid credentials"));
    }

    @Test
    void shouldNotAllowClientToChooseAdminRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "user@example.com",
                              "password": "StrongPassword123!",
                              "role": "ADMIN"
                            }
                            """))
                .andExpect(status().isCreated());

        User user = userRepository
                .findByEmail("user@example.com")
                .orElseThrow();

        assertEquals(UserRole.USER, user.getRole());
    }

    @Test
    void shouldRejectLoginWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "samuel@example.com",
                              "password": "StrongPassword123!"
                            }
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnAuthenticatedUser() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(user.getId().toString())
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("samuel@example.com")
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("USER")
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                );
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthenticationCookieIsMissing()
            throws Exception {

        mockMvc.perform(
                        get("/api/auth/me")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthenticationTokenIsInvalid()
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
}