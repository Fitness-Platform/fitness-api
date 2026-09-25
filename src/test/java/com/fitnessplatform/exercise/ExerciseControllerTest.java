package com.fitnessplatform.exercise;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.JwtService;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExerciseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        exerciseRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateExerciseAsAdmin() throws Exception {
        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post("/api/admin/exercises")
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "  Barbell Back Squat  ",
                                          "instructions": "  Descend under control.  ",
                                          "equipment": "  Barbell  ",
                                          "videoUrl": "  https://example.com/back-squat  "
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(
                        jsonPath("$.name")
                                .value("Barbell Back Squat")
                )
                .andExpect(
                        jsonPath("$.instructions")
                                .value("Descend under control.")
                )
                .andExpect(
                        jsonPath("$.equipment")
                                .value("Barbell")
                )
                .andExpect(
                        jsonPath("$.videoUrl")
                                .value(
                                        "https://example.com/back-squat"
                                )
                )
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertTrue(
                exerciseRepository.findAll()
                        .stream()
                        .anyMatch(exercise ->
                                exercise.getName()
                                        .equals("Barbell Back Squat")
                        )
        );
    }

    @Test
    void shouldRejectInvalidExercise() throws Exception {
        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post("/api/admin/exercises")
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "   ",
                                          "instructions": "Instructions"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        assertTrue(exerciseRepository.findAll().isEmpty());
    }

    @Test
    void shouldListExercisesAsAdmin() throws Exception {
        exerciseRepository.saveAndFlush(
                new Exercise(
                        "Bench Press",
                        "Lower the bar under control.",
                        "Barbell",
                        null
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get("/api/admin/exercises")
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Bench Press"))
                .andExpect(jsonPath("$[0].equipment")
                        .value("Barbell"));
    }

    @Test
    void shouldReturnExerciseByIdAsAdmin()
            throws Exception {

        Exercise exercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Push Up",
                                "Keep the body aligned.",
                                null,
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/exercises/{exerciseId}",
                                exercise.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(exercise.getId().toString())
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Push Up")
                )
                .andExpect(
                        jsonPath("$.equipment")
                                .doesNotExist()
                );
    }

    @Test
    void shouldReturnNotFoundWhenExerciseDoesNotExist()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/exercises/{exerciseId}",
                                java.util.UUID.randomUUID()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Exercise not found")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }

    @Test
    void shouldUpdateExerciseAsAdmin()
            throws Exception {

        Exercise exercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Bench Press",
                                "Old instructions",
                                "Barbell",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/exercises/{exerciseId}",
                                exercise.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Incline Bench Press",
                                          "instructions": "Use an incline bench.",
                                          "equipment": "Barbell",
                                          "videoUrl": "https://example.com/incline"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.name")
                                .value("Incline Bench Press")
                )
                .andExpect(
                        jsonPath("$.instructions")
                                .value("Use an incline bench.")
                )
                .andExpect(
                        jsonPath("$.videoUrl")
                                .value(
                                        "https://example.com/incline"
                                )
                );

        Exercise updatedExercise =
                exerciseRepository
                        .findById(exercise.getId())
                        .orElseThrow();

        assertTrue(
                updatedExercise.getName()
                        .equals("Incline Bench Press")
        );
    }

    @Test
    void shouldDeleteExerciseAsAdmin()
            throws Exception {

        Exercise exercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Bench Press",
                                null,
                                "Barbell",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/exercises/{exerciseId}",
                                exercise.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNoContent());

        assertFalse(
                exerciseRepository.existsById(
                        exercise.getId()
                )
        );
    }

    @Test
    void shouldRejectUserAccessToExerciseManagement()
            throws Exception {

        Cookie userCookie =
                authenticatedCookie(UserRole.USER);

        mockMvc.perform(
                        get("/api/admin/exercises")
                                .cookie(userCookie)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedAccessToExerciseManagement()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/exercises")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectExerciseMutationWithoutCsrf()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post("/api/admin/exercises")
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Bench Press"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        assertTrue(exerciseRepository.findAll().isEmpty());
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingExercise()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/exercises/{exerciseId}",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "name": "Bench Press",
                                      "instructions": "Instructions",
                                      "equipment": "Barbell",
                                      "videoUrl": null
                                    }
                                    """)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Exercise not found")
                );
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingExercise()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/exercises/{exerciseId}",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Exercise not found")
                );
    }

    private Cookie authenticatedCookie(
            UserRole role
    ) {
        User user = new User(
                role.name().toLowerCase()
                        + "@example.com",
                passwordEncoder.encode(
                        "StrongPassword123!"
                ),
                role
        );

        user = userRepository.saveAndFlush(user);

        String token =
                jwtService.generateToken(user.getId());

        return new Cookie(
                "AUTH_TOKEN",
                token
        );
    }
}