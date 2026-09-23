package com.fitnessplatform.auth;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}