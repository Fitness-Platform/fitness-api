package com.fitnessplatform.workout;

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
class WorkoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkoutRepository workoutRepository;

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
        workoutRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateWorkoutAsAdmin() throws Exception {
        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post("/api/admin/workouts")
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "  Upper Body Strength  ",
                                          "description": "  Strength-focused upper body workout.  "
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(
                        jsonPath("$.name")
                                .value("Upper Body Strength")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Strength-focused upper body workout.")
                )
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertTrue(
                workoutRepository.findAll()
                        .stream()
                        .anyMatch(workout ->
                                workout.getName()
                                        .equals("Upper Body Strength")
                        )
        );
    }

    @Test
    void shouldRejectInvalidWorkout() throws Exception {
        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post("/api/admin/workouts")
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "   ",
                                          "description": "Workout"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        assertTrue(
                workoutRepository.findAll().isEmpty()
        );
    }

    @Test
    void shouldListWorkoutsAsAdmin() throws Exception {
        workoutRepository.saveAndFlush(
                new Workout(
                        "Upper Body Strength",
                        "Upper body workout"
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get("/api/admin/workouts")
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].name")
                                .value("Upper Body Strength")
                )
                .andExpect(
                        jsonPath("$[0].description")
                                .value("Upper body workout")
                );
    }

    @Test
    void shouldReturnWorkoutByIdAsAdmin()
            throws Exception {

        Workout workout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Lower Body Strength",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/workouts/{workoutId}",
                                workout.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(workout.getId().toString())
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Lower Body Strength")
                );
    }

    @Test
    void shouldReturnNotFoundWhenWorkoutDoesNotExist()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/workouts/{workoutId}",
                                UUID.randomUUID()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Workout not found")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }

    @Test
    void shouldUpdateWorkoutAsAdmin()
            throws Exception {

        Workout workout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Upper Body",
                                "Old description"
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/workouts/{workoutId}",
                                workout.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Upper Body Strength",
                                          "description": "Updated description"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.name")
                                .value("Upper Body Strength")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Updated description")
                );

        Workout updatedWorkout =
                workoutRepository
                        .findById(workout.getId())
                        .orElseThrow();

        assertTrue(
                updatedWorkout.getName()
                        .equals("Upper Body Strength")
        );
    }

    @Test
    void shouldDeleteWorkoutAsAdmin()
            throws Exception {

        Workout workout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Upper Body",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/workouts/{workoutId}",
                                workout.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNoContent());

        assertFalse(
                workoutRepository.existsById(
                        workout.getId()
                )
        );
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingWorkout()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/workouts/{workoutId}",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Upper Body Strength",
                                          "description": "Workout"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Workout not found")
                );
    }

    @Test
    void shouldReturnNotFoundWhenDeletingMissingWorkout()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/workouts/{workoutId}",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Workout not found")
                );
    }

    @Test
    void shouldRejectUserAccessToWorkoutManagement()
            throws Exception {

        Cookie userCookie =
                authenticatedCookie(UserRole.USER);

        mockMvc.perform(
                        get("/api/admin/workouts")
                                .cookie(userCookie)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedAccessToWorkoutManagement()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/workouts")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWorkoutMutationWithoutCsrf()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post("/api/admin/workouts")
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Upper Body Strength"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        assertTrue(
                workoutRepository.findAll().isEmpty()
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