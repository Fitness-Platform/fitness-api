package com.fitnessplatform.program;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import com.fitnessplatform.workout.Workout;
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
class ProgramWeekWorkoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ProgramWeekRepository programWeekRepository;

    @Autowired
    private ProgramWeekWorkoutRepository programWeekWorkoutRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private WorkoutExerciseRepository workoutExerciseRepository;

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
        programWeekWorkoutRepository.deleteAll();
        programWeekRepository.deleteAll();
        programRepository.deleteAll();

        workoutExerciseRepository.deleteAll();
        workoutRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldAddWorkoutToProgramWeekAsAdmin()
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
                                "Upper Body",
                                "Upper body workout"
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                program.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "workoutId": "%s",
                                          "position": 1
                                        }
                                        """.formatted(
                                        workout.getId()
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(
                        jsonPath("$.programWeekId")
                                .value(week.getId().toString())
                )
                .andExpect(
                        jsonPath("$.workoutId")
                                .value(workout.getId().toString())
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                )
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertTrue(
                programWeekWorkoutRepository
                        .findAllByProgramWeekIdOrderByPositionAsc(
                                week.getId()
                        )
                        .size() == 1
        );
    }

    @Test
    void shouldRejectInvalidProgramWeekWorkout()
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
                        post(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                program.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "workoutId": null,
                                          "position": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        assertTrue(
                programWeekWorkoutRepository
                        .findAll()
                        .isEmpty()
        );
    }

    @Test
    void shouldReturnNotFoundWhenWorkoutDoesNotExist()
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
                        post(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                program.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "workoutId": "%s",
                                          "position": 1
                                        }
                                        """.formatted(
                                        UUID.randomUUID()
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Workout not found")
                );
    }

    @Test
    void shouldRejectProgramWeekFromDifferentProgramOnCreate()
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

        ProgramWeek weekA =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                programA,
                                "Week A",
                                null,
                                1
                        )
                );

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
                        post(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                programB.getId(),
                                weekA.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "workoutId": "%s",
                                          "position": 1
                                        }
                                        """.formatted(
                                        workout.getId()
                                ))
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program week not found")
                );

        assertTrue(
                programWeekWorkoutRepository
                        .findAll()
                        .isEmpty()
        );
    }

    @Test
    void shouldListWorkoutsOrderedByPosition()
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

        Workout upper =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Upper Body",
                                null
                        )
                );

        Workout lower =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Lower Body",
                                null
                        )
                );

        programWeekWorkoutRepository.saveAndFlush(
                new ProgramWeekWorkout(
                        week,
                        lower,
                        2
                )
        );

        programWeekWorkoutRepository.saveAndFlush(
                new ProgramWeekWorkout(
                        week,
                        upper,
                        1
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                program.getId(),
                                week.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].workoutId")
                                .value(upper.getId().toString())
                )
                .andExpect(
                        jsonPath("$[0].position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[1].workoutId")
                                .value(lower.getId().toString())
                )
                .andExpect(
                        jsonPath("$[1].position")
                                .value(2)
                );
    }

    @Test
    void shouldReturnEmptyListForWeekWithoutWorkouts()
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
                        get(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                program.getId(),
                                week.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnProgramWeekWorkoutById()
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
                                "Upper Body",
                                null
                        )
                );

        ProgramWeekWorkout association =
                programWeekWorkoutRepository.saveAndFlush(
                        new ProgramWeekWorkout(
                                week,
                                workout,
                                1
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(association.getId().toString())
                )
                .andExpect(
                        jsonPath("$.workoutId")
                                .value(workout.getId().toString())
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                );
    }

    @Test
    void shouldRejectAssociationFromDifferentProgramWeekOnGet()
            throws Exception {

        TestScenario scenario =
                createDifferentWeekScenario();

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                scenario.program().getId(),
                                scenario.otherWeek().getId(),
                                scenario.association().getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week workout not found"
                                )
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

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 1",
                                null,
                                1
                        )
                );

        Workout firstWorkout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Upper Body",
                                null
                        )
                );

        Workout secondWorkout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Lower Body",
                                null
                        )
                );

        programWeekWorkoutRepository.saveAndFlush(
                new ProgramWeekWorkout(
                        week,
                        firstWorkout,
                        1
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                program.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "workoutId": "%s",
                                          "position": 1
                                        }
                                        """.formatted(
                                        secondWorkout.getId()
                                ))
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week workout position conflict"
                                )
                );
    }

    @Test
    void shouldAllowSameWorkoutMultipleTimesInSameWeek()
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
                                "Upper Body",
                                null
                        )
                );

        programWeekWorkoutRepository.saveAndFlush(
                new ProgramWeekWorkout(
                        week,
                        workout,
                        1
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                program.getId(),
                                week.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "workoutId": "%s",
                                          "position": 2
                                        }
                                        """.formatted(
                                        workout.getId()
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.workoutId")
                                .value(workout.getId().toString())
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(2)
                );
    }

    @Test
    void shouldUpdateAssociationPosition()
            throws Exception {

        ProgramWeekWorkoutFixture fixture =
                createFixture();

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                fixture.program().getId(),
                                fixture.week().getId(),
                                fixture.association().getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "position": 2
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.workoutId")
                                .value(
                                        fixture.workout()
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(2)
                );
    }

    @Test
    void shouldAllowAssociationToKeepItsOwnPosition()
            throws Exception {

        ProgramWeekWorkoutFixture fixture =
                createFixture();

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                fixture.program().getId(),
                                fixture.week().getId(),
                                fixture.association().getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
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

        ProgramWeekWorkoutFixture fixture =
                createFixture();

        Workout secondWorkout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Lower Body",
                                null
                        )
                );

        programWeekWorkoutRepository.saveAndFlush(
                new ProgramWeekWorkout(
                        fixture.week(),
                        secondWorkout,
                        2
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                fixture.program().getId(),
                                fixture.week().getId(),
                                fixture.association().getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "position": 2
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week workout position conflict"
                                )
                );
    }

    @Test
    void shouldRejectUpdatingAssociationThroughDifferentWeek()
            throws Exception {

        TestScenario scenario =
                createDifferentWeekScenario();

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                scenario.program().getId(),
                                scenario.otherWeek().getId(),
                                scenario.association().getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "position": 3
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week workout not found"
                                )
                );

        ProgramWeekWorkout persisted =
                programWeekWorkoutRepository
                        .findById(
                                scenario.association().getId()
                        )
                        .orElseThrow();

        assertTrue(
                persisted.getPosition() == 1
        );
    }

    @Test
    void shouldDeleteProgramWeekWorkout()
            throws Exception {

        ProgramWeekWorkoutFixture fixture =
                createFixture();

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                fixture.program().getId(),
                                fixture.week().getId(),
                                fixture.association().getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNoContent());

        assertFalse(
                programWeekWorkoutRepository.existsById(
                        fixture.association().getId()
                )
        );
    }

    @Test
    void shouldRejectDeletingAssociationThroughDifferentWeek()
            throws Exception {

        TestScenario scenario =
                createDifferentWeekScenario();

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                scenario.program().getId(),
                                scenario.otherWeek().getId(),
                                scenario.association().getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week workout not found"
                                )
                );

        assertTrue(
                programWeekWorkoutRepository.existsById(
                        scenario.association().getId()
                )
        );
    }

    @Test
    void shouldDeleteAssociationsWhenProgramWeekIsDeleted() {

        ProgramWeekWorkoutFixture fixture =
                createFixture();

        UUID associationId =
                fixture.association().getId();

        programWeekRepository.delete(
                fixture.week()
        );

        programWeekRepository.flush();

        assertFalse(
                programWeekWorkoutRepository.existsById(
                        associationId
                )
        );
    }

    @Test
    void shouldPreventDeletingWorkoutUsedByProgramWeek()
            throws Exception {

        ProgramWeekWorkoutFixture fixture =
                createFixture();

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/workouts/{workoutId}",
                                fixture.workout().getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value("Workout is in use")
                );

        assertTrue(
                workoutRepository.existsById(
                        fixture.workout().getId()
                )
        );

        assertTrue(
                programWeekWorkoutRepository.existsById(
                        fixture.association().getId()
                )
        );
    }

    @Test
    void shouldRejectUserAccessToProgramWeekWorkouts()
            throws Exception {

        ProgramWeekWorkoutFixture fixture =
                createFixture();

        Cookie userCookie =
                authenticatedCookie(UserRole.USER);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                fixture.program().getId(),
                                fixture.week().getId()
                        )
                                .cookie(userCookie)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedAccess()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts",
                                UUID.randomUUID(),
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMutationWithoutCsrf()
            throws Exception {

        ProgramWeekWorkoutFixture fixture =
                createFixture();

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                fixture.program().getId(),
                                fixture.week().getId(),
                                fixture.association().getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isForbidden());

        assertTrue(
                programWeekWorkoutRepository.existsById(
                        fixture.association().getId()
                )
        );
    }

    private ProgramWeekWorkoutFixture createFixture() {

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
                                "Upper Body",
                                null
                        )
                );

        ProgramWeekWorkout association =
                programWeekWorkoutRepository.saveAndFlush(
                        new ProgramWeekWorkout(
                                week,
                                workout,
                                1
                        )
                );

        return new ProgramWeekWorkoutFixture(
                program,
                week,
                workout,
                association
        );
    }

    private TestScenario createDifferentWeekScenario() {

        ProgramWeekWorkoutFixture fixture =
                createFixture();

        ProgramWeek otherWeek =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                fixture.program(),
                                "Week 2",
                                null,
                                2
                        )
                );

        return new TestScenario(
                fixture.program(),
                otherWeek,
                fixture.association()
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

    private record ProgramWeekWorkoutFixture(
            Program program,
            ProgramWeek week,
            Workout workout,
            ProgramWeekWorkout association
    ) {
    }

    private record TestScenario(
            Program program,
            ProgramWeek otherWeek,
            ProgramWeekWorkout association
    ) {
    }
}