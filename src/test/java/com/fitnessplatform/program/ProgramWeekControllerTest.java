package com.fitnessplatform.program;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.*;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.exercise.Exercise;
import com.fitnessplatform.exercise.ExerciseRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import com.fitnessplatform.workout.Workout;
import com.fitnessplatform.workout.WorkoutExercise;
import com.fitnessplatform.workout.WorkoutExerciseRepository;
import com.fitnessplatform.workout.WorkoutRepository;
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
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
class ProgramWeekControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ProgramWeekRepository programWeekRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private ProgramWeekWorkoutRepository
            programWeekWorkoutRepository;

    @Autowired
    private WorkoutRepository
            workoutRepository;

    @Autowired
    private WorkoutExerciseRepository
            workoutExerciseRepository;

    @Autowired
    private ExerciseRepository
            exerciseRepository;

    @Autowired
    private ProgramAccessRepository
            programAccessRepository;

    @Autowired
    private ProgramAccessWorkoutRepository
            programAccessWorkoutRepository;

    @Autowired
    private ProgramAccessWorkoutSetRepository
            programAccessWorkoutSetRepository;

    @Autowired
    private ProgramAccessWorkoutTrackingService
            programAccessWorkoutTrackingService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        programAccessWorkoutSetRepository.deleteAll();
        programAccessWorkoutRepository.deleteAll();
        programAccessRepository.deleteAll();

        programWeekWorkoutRepository.deleteAll();
        workoutExerciseRepository.deleteAll();

        programWeekRepository.deleteAll();

        workoutRepository.deleteAll();
        exerciseRepository.deleteAll();

        programRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateProgramWeekAsAdmin()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/weeks",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "  Week 1  ",
                                          "description": "  Foundation week.  ",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(
                        jsonPath("$.programId")
                                .value(program.getId().toString())
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Week 1")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Foundation week.")
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                )
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertTrue(
                programWeekRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                program.getId()
                        )
                        .size() == 1
        );
    }

    @Test
    void shouldRejectInvalidProgramWeek()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/weeks",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "   ",
                                          "description": "Invalid week",
                                          "position": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        assertTrue(
                programWeekRepository.findAll().isEmpty()
        );
    }

    @Test
    void shouldReturnNotFoundWhenCreatingWeekForMissingProgram()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/weeks",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Week 1",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program not found")
                );
    }

    @Test
    void shouldListProgramWeeksOrderedByPosition()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        programWeekRepository.saveAndFlush(
                new ProgramWeek(
                        program,
                        "Week 2",
                        null,
                        2
                )
        );

        programWeekRepository.saveAndFlush(
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks",
                                program.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].title")
                                .value("Week 1")
                )
                .andExpect(
                        jsonPath("$[0].position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[1].title")
                                .value("Week 2")
                )
                .andExpect(
                        jsonPath("$[1].position")
                                .value(2)
                );
    }

    @Test
    void shouldReturnEmptyListForProgramWithoutWeeks()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Empty Program",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks",
                                program.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnNotFoundWhenListingWeeksForMissingProgram()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks",
                                UUID.randomUUID()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program not found")
                );
    }

    @Test
    void shouldReturnProgramWeekById()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 1",
                                "Foundation",
                                1
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(week.getId().toString())
                )
                .andExpect(
                        jsonPath("$.programId")
                                .value(program.getId().toString())
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Week 1")
                );
    }

    @Test
    void shouldRejectWeekFromDifferentProgram()
            throws Exception {

        Program programA =
                programRepository.saveAndFlush(
                        new Program(
                                "Program A",
                                null
                        )
                );

        Program programB =
                programRepository.saveAndFlush(
                        new Program(
                                "Program B",
                                null
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                programA,
                                "Week A",
                                null,
                                1
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks/{weekId}",
                                programB.getId(),
                                week.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program week not found")
                );
    }

    @Test
    void shouldRejectDuplicatePositionOnCreate()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        programWeekRepository.saveAndFlush(
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/weeks",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Another Week",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week position conflict"
                                )
                );
    }

    @Test
    void shouldUpdateProgramWeek()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Old Week",
                                "Old description",
                                1
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Updated Week",
                                          "description": "Updated description",
                                          "position": 2
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.title")
                                .value("Updated Week")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Updated description")
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(2)
                );
    }

    @Test
    void shouldAllowWeekToKeepItsOwnPositionOnUpdate()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 1",
                                null,
                                1
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Updated Week 1",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                );
    }

    @Test
    void shouldRejectDuplicatePositionOnUpdate()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        programWeekRepository.saveAndFlush(
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                )
        );

        ProgramWeek weekTwo =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 2",
                                null,
                                2
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                weekTwo.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Week 2",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week position conflict"
                                )
                );
    }

    @Test
    void shouldDeleteProgramWeek()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 1",
                                null,
                                1
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNoContent());

        assertFalse(
                programWeekRepository.existsById(
                        week.getId()
                )
        );
    }

    @Test
    void shouldRejectDeletingProgramWeekWithTrackingHistory()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 1",
                                null,
                                1
                        )
                );

        Workout workout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Full Body Strength",
                                null
                        )
                );

        ProgramWeekWorkout programWeekWorkout =
                programWeekWorkoutRepository
                        .saveAndFlush(
                                new ProgramWeekWorkout(
                                        week,
                                        workout,
                                        1
                                )
                        );

        Exercise exercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Goblet Squat",
                                null,
                                "Dumbbell",
                                null
                        )
                );

        WorkoutExercise workoutExercise =
                workoutExerciseRepository
                        .saveAndFlush(
                                new WorkoutExercise(
                                        workout,
                                        exercise,
                                        1,
                                        "10",
                                        new BigDecimal("35.00"),
                                        90,
                                        null,
                                        1
                                )
                        );

        User member =
                userRepository.saveAndFlush(
                        new User(
                                "member-history@example.com",
                                passwordEncoder.encode(
                                        "StrongPassword123!"
                                ),
                                UserRole.USER
                        )
                );

        ProgramAccess access =
                programAccessRepository
                        .saveAndFlush(
                                new ProgramAccess(
                                        member,
                                        program,
                                        Instant.parse(
                                                "2026-09-01T12:00:00Z"
                                        ),
                                        null
                                )
                        );

        ProgramAccessWorkoutSet savedSet =
                programAccessWorkoutTrackingService
                        .saveSet(
                                access,
                                programWeekWorkout,
                                workoutExercise,
                                1,
                                10,
                                new BigDecimal("35.00")
                        );

        ProgramAccessWorkout accessWorkout =
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                access.getId(),
                                programWeekWorkout.getId()
                        )
                        .orElseThrow();

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        delete(
                                "/api/admin/programs/{programId}"
                                        + "/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week is in use"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                );

        assertTrue(
                programWeekRepository.existsById(
                        week.getId()
                )
        );

        assertTrue(
                programWeekWorkoutRepository.existsById(
                        programWeekWorkout.getId()
                )
        );

        assertTrue(
                programAccessWorkoutRepository.existsById(
                        accessWorkout.getId()
                )
        );

        assertTrue(
                programAccessWorkoutSetRepository.existsById(
                        savedSet.getId()
                )
        );
    }

    @Test
    void shouldDeleteWeeksWhenProgramIsDeleted() {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 1",
                                null,
                                1
                        )
                );

        UUID weekId = week.getId();

        programRepository.delete(program);
        programRepository.flush();

        assertFalse(
                programWeekRepository.existsById(
                        weekId
                )
        );
    }

    @Test
    void shouldRejectUserAccessToProgramWeeks()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        Cookie userCookie =
                authenticatedCookie(UserRole.USER);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks",
                                program.getId()
                        )
                                .cookie(userCookie)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedAccessToProgramWeeks()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks",
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectProgramWeekMutationWithoutCsrf()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/weeks",
                                program.getId()
                        )
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Week 1",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        assertTrue(
                programWeekRepository.findAll().isEmpty()
        );
    }

    @Test
    void shouldRejectUpdatingWeekThroughDifferentProgram()
            throws Exception {

        Program programA =
                programRepository.saveAndFlush(
                        new Program(
                                "Program A",
                                null
                        )
                );

        Program programB =
                programRepository.saveAndFlush(
                        new Program(
                                "Program B",
                                null
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                programA,
                                "Original Week",
                                "Original description",
                                1
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/weeks/{weekId}",
                                programB.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "title": "Modified Week",
                                      "description": "Modified description",
                                      "position": 2
                                    }
                                    """)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program week not found")
                );

        ProgramWeek persisted =
                programWeekRepository
                        .findById(week.getId())
                        .orElseThrow();

        assertEquals(
                "Original Week",
                persisted.getTitle()
        );

        assertEquals(
                "Original description",
                persisted.getDescription()
        );

        assertEquals(
                1,
                persisted.getPosition()
        );
    }

    @Test
    void shouldRejectDeletingWeekThroughDifferentProgram()
            throws Exception {

        Program programA =
                programRepository.saveAndFlush(
                        new Program(
                                "Program A",
                                null
                        )
                );

        Program programB =
                programRepository.saveAndFlush(
                        new Program(
                                "Program B",
                                null
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                programA,
                                "Week 1",
                                null,
                                1
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/programs/{programId}/weeks/{weekId}",
                                programB.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program week not found")
                );

        assertTrue(
                programWeekRepository.existsById(
                        week.getId()
                )
        );
    }

    private Cookie authenticatedCookie(
            UserRole role
    ) {
        User user =
                new User(
                        role.name().toLowerCase()
                                + "@example.com",
                        passwordEncoder.encode(
                                "StrongPassword123!"
                        ),
                        role
                );

        user =
                userRepository.saveAndFlush(user);

        String token =
                jwtService.generateToken(
                        user.getId()
                );

        return new Cookie(
                "AUTH_TOKEN",
                token
        );
    }
}