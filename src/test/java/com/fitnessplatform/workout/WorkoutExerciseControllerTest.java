package com.fitnessplatform.workout;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.exercise.Exercise;
import com.fitnessplatform.exercise.ExerciseRepository;
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

import java.math.BigDecimal;
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
class WorkoutExerciseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkoutExerciseRepository workoutExerciseRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

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
        workoutExerciseRepository.deleteAll();
        workoutRepository.deleteAll();
        exerciseRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldAddExerciseToWorkoutAsAdmin()
            throws Exception {

        Workout workout = createWorkout(
                "Upper Body Strength"
        );

        Exercise exercise = createExercise(
                "Bench Press"
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/workouts/{workoutId}/exercises",
                                workout.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "exerciseId": "%s",
                                          "sets": 3,
                                          "reps": "8-10",
                                          "suggestedWeightLb": 40.00,
                                          "restSeconds": 90,
                                          "notes": "Control the eccentric.",
                                          "position": 1
                                        }
                                        """.formatted(exercise.getId()))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(
                        jsonPath("$.workoutId")
                                .value(workout.getId().toString())
                )
                .andExpect(
                        jsonPath("$.exerciseId")
                                .value(exercise.getId().toString())
                )
                .andExpect(jsonPath("$.sets").value(3))
                .andExpect(jsonPath("$.reps").value("8-10"))
                .andExpect(
                        jsonPath("$.suggestedWeightLb")
                                .value(40.0)
                )
                .andExpect(
                        jsonPath("$.restSeconds")
                                .value(90)
                )
                .andExpect(
                        jsonPath("$.notes")
                                .value("Control the eccentric.")
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                );
    }

    @Test
    void shouldListWorkoutExercisesOrderedByPosition()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Exercise benchPress =
                createExercise("Bench Press");

        Exercise dumbbellRow =
                createExercise("Dumbbell Row");

        createWorkoutExercise(
                workout,
                dumbbellRow,
                2
        );

        createWorkoutExercise(
                workout,
                benchPress,
                1
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/workouts/{workoutId}/exercises",
                                workout.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(
                        jsonPath("$[0].exerciseId")
                                .value(
                                        benchPress.getId().toString()
                                )
                )
                .andExpect(jsonPath("$[1].position").value(2))
                .andExpect(
                        jsonPath("$[1].exerciseId")
                                .value(
                                        dumbbellRow.getId().toString()
                                )
                );
    }

    @Test
    void shouldReturnWorkoutExerciseById()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Exercise exercise =
                createExercise("Bench Press");

        WorkoutExercise workoutExercise =
                createWorkoutExercise(
                        workout,
                        exercise,
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/workouts/{workoutId}/exercises/{workoutExerciseId}",
                                workout.getId(),
                                workoutExercise.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        workoutExercise
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.exerciseId")
                                .value(
                                        exercise.getId().toString()
                                )
                );
    }

    @Test
    void shouldUpdateWorkoutExerciseAsAdmin()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Exercise exercise =
                createExercise("Bench Press");

        WorkoutExercise workoutExercise =
                createWorkoutExercise(
                        workout,
                        exercise,
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/workouts/{workoutId}/exercises/{workoutExerciseId}",
                                workout.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sets": 4,
                                          "reps": "6-8",
                                          "suggestedWeightLb": 50.00,
                                          "restSeconds": 120,
                                          "notes": "Increase load gradually.",
                                          "position": 2
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets").value(4))
                .andExpect(jsonPath("$.reps").value("6-8"))
                .andExpect(
                        jsonPath("$.suggestedWeightLb")
                                .value(50.0)
                )
                .andExpect(
                        jsonPath("$.restSeconds")
                                .value(120)
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.exerciseId")
                                .value(
                                        exercise.getId().toString()
                                )
                );
    }

    @Test
    void shouldDeleteWorkoutExerciseAsAdmin()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Exercise exercise =
                createExercise("Bench Press");

        WorkoutExercise workoutExercise =
                createWorkoutExercise(
                        workout,
                        exercise,
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/workouts/{workoutId}/exercises/{workoutExerciseId}",
                                workout.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNoContent());

        assertFalse(
                workoutExerciseRepository.existsById(
                        workoutExercise.getId()
                )
        );
    }

    @Test
    void shouldRejectDuplicatePosition()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Exercise benchPress =
                createExercise("Bench Press");

        Exercise row =
                createExercise("Dumbbell Row");

        createWorkoutExercise(
                workout,
                benchPress,
                1
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/workouts/{workoutId}/exercises",
                                workout.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "exerciseId": "%s",
                                          "sets": 3,
                                          "reps": "10",
                                          "restSeconds": 60,
                                          "position": 1
                                        }
                                        """.formatted(row.getId()))
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Workout exercise position conflict"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                );
    }

    @Test
    void shouldRejectPositionConflictWhenUpdating()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Exercise benchPress =
                createExercise("Bench Press");

        Exercise row =
                createExercise("Dumbbell Row");

        WorkoutExercise first =
                createWorkoutExercise(
                        workout,
                        benchPress,
                        1
                );

        createWorkoutExercise(
                workout,
                row,
                2
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/workouts/{workoutId}/exercises/{workoutExerciseId}",
                                workout.getId(),
                                first.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "sets": 4,
                                          "reps": "8",
                                          "restSeconds": 90,
                                          "position": 2
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Workout exercise position conflict"
                                )
                );
    }

    @Test
    void shouldRejectWorkoutExerciseFromDifferentWorkout()
            throws Exception {

        Workout firstWorkout =
                createWorkout("Workout A");

        Workout secondWorkout =
                createWorkout("Workout B");

        Exercise exercise =
                createExercise("Bench Press");

        WorkoutExercise workoutExercise =
                createWorkoutExercise(
                        secondWorkout,
                        exercise,
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/workouts/{workoutId}/exercises/{workoutExerciseId}",
                                firstWorkout.getId(),
                                workoutExercise.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Workout exercise not found"
                                )
                );
    }

    @Test
    void shouldReturnNotFoundWhenWorkoutDoesNotExist()
            throws Exception {

        Exercise exercise =
                createExercise("Bench Press");

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/workouts/{workoutId}/exercises",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "exerciseId": "%s",
                                          "sets": 3,
                                          "reps": "10",
                                          "position": 1
                                        }
                                        """.formatted(exercise.getId()))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Workout not found")
                );
    }

    @Test
    void shouldReturnNotFoundWhenExerciseDoesNotExist()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/workouts/{workoutId}/exercises",
                                workout.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "exerciseId": "%s",
                                          "sets": 3,
                                          "reps": "10",
                                          "position": 1
                                        }
                                        """.formatted(UUID.randomUUID()))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Exercise not found")
                );
    }

    @Test
    void shouldRejectInvalidWorkoutExercise()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Exercise exercise =
                createExercise("Bench Press");

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/workouts/{workoutId}/exercises",
                                workout.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "exerciseId": "%s",
                                          "sets": 0,
                                          "reps": "",
                                          "suggestedWeightLb": -10,
                                          "restSeconds": -1,
                                          "position": 0
                                        }
                                        """.formatted(exercise.getId()))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUserAccessToWorkoutExerciseManagement()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Cookie userCookie =
                authenticatedCookie(UserRole.USER);

        mockMvc.perform(
                        get(
                                "/api/admin/workouts/{workoutId}/exercises",
                                workout.getId()
                        )
                                .cookie(userCookie)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedAccessToWorkoutExerciseManagement()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        mockMvc.perform(
                        get(
                                "/api/admin/workouts/{workoutId}/exercises",
                                workout.getId()
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWorkoutExerciseMutationWithoutCsrf()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Exercise exercise =
                createExercise("Bench Press");

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/workouts/{workoutId}/exercises",
                                workout.getId()
                        )
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "exerciseId": "%s",
                                          "sets": 3,
                                          "reps": "10",
                                          "position": 1
                                        }
                                        """.formatted(exercise.getId()))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldPreventDeletingExerciseUsedByWorkout()
            throws Exception {

        Workout workout =
                createWorkout("Upper Body");

        Exercise exercise =
                createExercise("Bench Press");

        createWorkoutExercise(
                workout,
                exercise,
                1
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
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value("Exercise is in use")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                );

        assertTrue(
                exerciseRepository.existsById(
                        exercise.getId()
                )
        );

        assertTrue(
                workoutExerciseRepository.existsById(
                        workoutExerciseRepository
                                .findAll()
                                .getFirst()
                                .getId()
                )
        );
    }

    private Workout createWorkout(
            String name
    ) {
        return workoutRepository.saveAndFlush(
                new Workout(
                        name,
                        null
                )
        );
    }

    private Exercise createExercise(
            String name
    ) {
        return exerciseRepository.saveAndFlush(
                new Exercise(
                        name,
                        null,
                        null,
                        null
                )
        );
    }

    private WorkoutExercise createWorkoutExercise(
            Workout workout,
            Exercise exercise,
            Integer position
    ) {
        return workoutExerciseRepository.saveAndFlush(
                new WorkoutExercise(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("40.00"),
                        90,
                        null,
                        position
                )
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