package com.fitnessplatform.auth;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.email.AuthenticationEmailService;
import com.fitnessplatform.auth.passwordreset.PasswordResetService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordResetService passwordResetService;

    @MockitoBean
    private AuthenticationEmailService authenticationEmailService;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        passwordResetTokenRepository.deleteAll();
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

        verify(authenticationEmailService)
                .sendWelcomeEmail(
                        "samuel@example.com"
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

        verifyNoInteractions(authenticationEmailService);
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

        verifyNoInteractions(authenticationEmailService);
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

    @Test
    void shouldLogoutAndExpireAuthenticationCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("AUTH_TOKEN=")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=0")
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
    void shouldLogoutWhenAuthenticationTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(
                                new Cookie(
                                        "AUTH_TOKEN",
                                        "invalid-token"
                                )
                        ))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("AUTH_TOKEN=")
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=0")
                ));
    }

    @Test
    void shouldRejectLogoutWithoutCsrf() throws Exception {
        mockMvc.perform(
                        post("/api/auth/logout")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptPasswordResetRequestForExistingEmail()
            throws Exception {

        User user = new User(
                "samuel@example.com",
                passwordEncoder.encode("StrongPassword123!"),
                UserRole.USER
        );

        userRepository.saveAndFlush(user);

        mockMvc.perform(
                        post("/api/auth/forgot-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "samuel@example.com"
                                    }
                                    """)
                )
                .andExpect(status().isAccepted());

        verify(authenticationEmailService)
                .sendPasswordResetEmail(
                        eq("samuel@example.com"),
                        argThat(token ->
                                token != null && !token.isBlank()
                        )
                );
    }

    @Test
    void shouldAcceptPasswordResetRequestForUnknownEmail()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/forgot-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "unknown@example.com"
                                    }
                                    """)
                )
                .andExpect(status().isAccepted());

        verifyNoInteractions(authenticationEmailService);
    }

    @Test
    void shouldResetPasswordWithValidToken() throws Exception {
        User user = new User(
                "samuel@example.com",
                passwordEncoder.encode("OldPassword123!"),
                UserRole.USER
        );

        userRepository.saveAndFlush(user);

        String rawToken =
                passwordResetService
                        .requestReset("samuel@example.com")
                        .orElseThrow()
                        .rawToken();

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "token": "%s",
                                      "newPassword": "NewPassword123!"
                                    }
                                    """.formatted(rawToken))
                )
                .andExpect(status().isNoContent());

        User updatedUser = userRepository
                .findByEmail("samuel@example.com")
                .orElseThrow();

        assertTrue(
                passwordEncoder.matches(
                        "NewPassword123!",
                        updatedUser.getPasswordHash()
                )
        );

        assertFalse(
                passwordEncoder.matches(
                        "OldPassword123!",
                        updatedUser.getPasswordHash()
                )
        );
    }

    @Test
    void shouldNotAllowPasswordResetTokenToBeReused()
            throws Exception {

        User user = new User(
                "samuel@example.com",
                passwordEncoder.encode("OldPassword123!"),
                UserRole.USER
        );

        userRepository.saveAndFlush(user);

        String rawToken =
                passwordResetService
                        .requestReset("samuel@example.com")
                        .orElseThrow()
                        .rawToken();

        String requestBody = """
            {
              "token": "%s",
              "newPassword": "NewPassword123!"
            }
            """.formatted(rawToken);

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.title")
                                .value("Invalid password reset token")
                );
    }

    @Test
    void shouldRejectInvalidPasswordResetToken()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "token": "does-not-exist",
                                      "newPassword": "NewPassword123!"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.title")
                                .value("Invalid password reset token")
                );
    }

    @Test
    void shouldAuthenticateOnlyWithNewPasswordAfterReset()
            throws Exception {

        User user = new User(
                "samuel@example.com",
                passwordEncoder.encode("OldPassword123!"),
                UserRole.USER
        );

        userRepository.saveAndFlush(user);

        String rawToken =
                passwordResetService
                        .requestReset("samuel@example.com")
                        .orElseThrow()
                        .rawToken();

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "token": "%s",
                                      "newPassword": "NewPassword123!"
                                    }
                                    """.formatted(rawToken))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "samuel@example.com",
                                      "password": "OldPassword123!"
                                    }
                                    """)
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "samuel@example.com",
                                      "password": "NewPassword123!"
                                    }
                                    """)
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectPasswordResetWithoutCsrf()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "token": "some-token",
                                      "newPassword": "NewPassword123!"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldInvalidatePreviousPasswordResetToken()
            throws Exception {

        User user = new User(
                "samuel@example.com",
                passwordEncoder.encode("OldPassword123!"),
                UserRole.USER
        );

        userRepository.saveAndFlush(user);

        String firstToken =
                passwordResetService
                        .requestReset("samuel@example.com")
                        .orElseThrow()
                        .rawToken();

        String secondToken =
                passwordResetService
                        .requestReset("samuel@example.com")
                        .orElseThrow()
                        .rawToken();

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "token": "%s",
                                      "newPassword": "NewPassword123!"
                                    }
                                    """.formatted(firstToken))
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/api/auth/reset-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "token": "%s",
                                      "newPassword": "NewPassword123!"
                                    }
                                    """.formatted(secondToken))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectForgotPasswordWithoutCsrf()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "samuel@example.com"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden());
    }
}