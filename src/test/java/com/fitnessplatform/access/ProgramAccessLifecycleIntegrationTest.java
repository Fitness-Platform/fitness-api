package com.fitnessplatform.access;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.program.ProgramWeek;
import com.fitnessplatform.program.ProgramWeekRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import com.fitnessplatform.exercise.Exercise;
import com.fitnessplatform.exercise.ExerciseRepository;
import com.fitnessplatform.program.ProgramWeekWorkout;
import com.fitnessplatform.program.ProgramWeekWorkoutRepository;
import com.fitnessplatform.workout.Workout;
import com.fitnessplatform.workout.WorkoutExercise;
import com.fitnessplatform.workout.WorkoutExerciseRepository;
import com.fitnessplatform.workout.WorkoutRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        TestcontainersConfiguration.class,
        ProgramAccessLifecycleIntegrationTest.FixedClockConfiguration.class
})
class ProgramAccessLifecycleIntegrationTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-10-01T12:00:00Z"
            );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgramAccessRepository
            programAccessRepository;

    @Autowired
    private ProgramRepository
            programRepository;

    @Autowired
    private ProgramWeekRepository
            programWeekRepository;

    @Autowired
    private UserRepository
            userRepository;

    @Autowired
    private PasswordResetTokenRepository
            passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder
            passwordEncoder;

    @Autowired
    private JwtService
            jwtService;

    @Autowired
    private ProgramAccessExerciseLoadRepository
            programAccessExerciseLoadRepository;

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

    @BeforeEach
    void cleanDatabase() {
        programAccessExerciseLoadRepository.deleteAll();

        programWeekWorkoutRepository.deleteAll();
        workoutExerciseRepository.deleteAll();

        programWeekRepository.deleteAll();

        programAccessRepository.deleteAll();

        exerciseRepository.deleteAll();
        workoutRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        programRepository.deleteAll();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    NOW,
                    ZoneOffset.UTC
            );
        }
    }

    @Test
    void shouldGrantAccessAsAdminAndDeliverProgramToMember()
            throws Exception {

        User member =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        User admin =
                createUser(
                        "admin@example.com",
                        UserRole.ADMIN
                );

        Program program =
                createPublishedProgram(
                        "Six Week Strength"
                );

        createWeek(
                program,
                "Week 1",
                1
        );

        createWeek(
                program,
                "Week 2",
                2
        );

        Cookie adminCookie =
                authenticatedCookie(admin);

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        post(
                                "/api/admin/program-accesses"
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "userId": "%s",
                                          "programId": "%s",
                                          "startsAt": "2026-10-01T12:00:00Z",
                                          "expiresAt": "2027-04-01T12:00:00Z"
                                        }
                                        """.formatted(
                                                member.getId(),
                                                program.getId()
                                        )
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(
                                        member.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.programId")
                                .value(
                                        program.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.startsAt")
                                .value(
                                        "2026-10-01T12:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.expiresAt")
                                .value(
                                        "2027-04-01T12:00:00Z"
                                )
                );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                program.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.programId")
                                .value(
                                        program.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.accessStartsAt")
                                .value(
                                        "2026-10-01T12:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.accessExpiresAt")
                                .value(
                                        "2027-04-01T12:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.weeks.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.weeks[0].position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.weeks[0].unlocked")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.weeks[0].unlocksAt")
                                .value(
                                        "2026-10-01T12:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.weeks[1].position")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.weeks[1].unlocked")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.weeks[1].unlocksAt")
                                .value(
                                        "2026-10-08T12:00:00Z"
                                )
                );
    }

    @Test
    void shouldKeepExerciseLoadsIsolatedBetweenProgramAccesses()
            throws Exception {

        User member =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Program program =
                createPublishedProgram(
                        "Strength Program"
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body"
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat"
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        new BigDecimal("25.00")
                );

        Cookie memberCookie =
                authenticatedCookie(member);

        ProgramAccess firstAccess =
                programAccessRepository.saveAndFlush(
                        new ProgramAccess(
                                member,
                                program,
                                Instant.parse(
                                        "2026-08-01T12:00:00Z"
                                ),
                                null
                        )
                );

        String loadPath =
                "/api/me/programs/{programId}"
                        + "/weeks/{weekId}"
                        + "/workouts/{associationId}"
                        + "/exercises/{workoutExerciseId}/load";

        mockMvc.perform(
                        put(
                                loadPath,
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(memberCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "weightLb": 30.00
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.currentWeightLb")
                                .value(30.00)
                );

        ProgramAccess secondAccess =
                programAccessRepository.saveAndFlush(
                        new ProgramAccess(
                                member,
                                program,
                                Instant.parse(
                                        "2026-09-15T12:00:00Z"
                                ),
                                null
                        )
                );

        mockMvc.perform(
                        put(
                                loadPath,
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(memberCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "weightLb": 40.00
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.currentWeightLb")
                                .value(40.00)
                );

        assertEquals(
                2,
                programAccessExerciseLoadRepository.count()
        );

        assertEquals(
                new BigDecimal("30.00"),
                programAccessExerciseLoadRepository
                        .findByProgramAccessIdAndWorkoutExerciseId(
                                firstAccess.getId(),
                                workoutExercise.getId()
                        )
                        .orElseThrow()
                        .getWeightLb()
        );

        assertEquals(
                new BigDecimal("40.00"),
                programAccessExerciseLoadRepository
                        .findByProgramAccessIdAndWorkoutExerciseId(
                                secondAccess.getId(),
                                workoutExercise.getId()
                        )
                        .orElseThrow()
                        .getWeightLb()
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].currentWeightLb"
                        )
                                .value(40.00)
                );

        secondAccess.revoke(
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                )
        );

        programAccessRepository.saveAndFlush(
                secondAccess
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].currentWeightLb"
                        )
                                .value(30.00)
                );
    }

    private User createUser(
            String email,
            UserRole role
    ) {
        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(
                                "StrongPassword123!"
                        ),
                        role
                )
        );
    }

    private Program createPublishedProgram(
            String name
    ) {
        Program program =
                new Program(
                        name,
                        null
                );

        program.publish();

        return programRepository.saveAndFlush(
                program
        );
    }

    private ProgramWeek createWeek(
            Program program,
            String title,
            Integer position
    ) {
        return programWeekRepository.saveAndFlush(
                new ProgramWeek(
                        program,
                        title,
                        null,
                        position
                )
        );
    }

    private Cookie authenticatedCookie(
            User user
    ) {
        String token =
                jwtService.generateToken(
                        user.getId()
                );

        return new Cookie(
                "AUTH_TOKEN",
                token
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

    private ProgramWeekWorkout addWorkoutToWeek(
            ProgramWeek week,
            Workout workout,
            Integer position
    ) {
        return programWeekWorkoutRepository
                .saveAndFlush(
                        new ProgramWeekWorkout(
                                week,
                                workout,
                                position
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
                        "Dumbbell",
                        null
                )
        );
    }

    private WorkoutExercise addExerciseToWorkout(
            Workout workout,
            Exercise exercise,
            BigDecimal suggestedWeightLb
    ) {
        return workoutExerciseRepository
                .saveAndFlush(
                        new WorkoutExercise(
                                workout,
                                exercise,
                                3,
                                "10",
                                suggestedWeightLb,
                                90,
                                null,
                                1
                        )
                );
    }
}