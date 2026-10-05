package com.fitnessplatform.program;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccess;
import com.fitnessplatform.access.ProgramAccessExerciseLoad;
import com.fitnessplatform.access.ProgramAccessExerciseLoadRepository;
import com.fitnessplatform.access.ProgramAccessRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
        TestcontainersConfiguration.class,
        MemberProgramControllerTest.FixedClockConfiguration.class
})
class MemberProgramControllerTest {

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
    private ProgramWeekRepository
            programWeekRepository;

    @Autowired
    private ProgramResourceRepository
            programResourceRepository;

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
    private ProgramAccessExerciseLoadRepository
            programAccessExerciseLoadRepository;

    @BeforeEach
    void cleanDatabase() {
        programAccessExerciseLoadRepository.deleteAll();

        programWeekWorkoutRepository.deleteAll();
        workoutExerciseRepository.deleteAll();

        programResourceRepository.deleteAll();
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
    void shouldListActiveProgramsForAuthenticatedMember()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Six Week Strength",
                        "Six-week strength program."
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                ),
                Instant.parse(
                        "2027-03-20T12:00:00Z"
                )
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].programId")
                                .value(
                                        program.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$[0].name")
                                .value(
                                        "Six Week Strength"
                                )
                )
                .andExpect(
                        jsonPath("$[0].description")
                                .value(
                                        "Six-week strength program."
                                )
                )
                .andExpect(
                        jsonPath("$[0].accessStartsAt")
                                .value(
                                        "2026-09-20T12:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$[0].accessExpiresAt")
                                .value(
                                        "2027-03-20T12:00:00Z"
                                )
                );
    }

    @Test
    void shouldReturnEmptyListWhenMemberHasNoPrograms()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );
    }

    @Test
    void shouldNotListProgramWithFutureAccess()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Future Program",
                        null
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-10-02T12:00:00Z"
                ),
                null
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );
    }

    @Test
    void shouldNotListProgramWithExpiredAccess()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Expired Program",
                        null
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-01-01T12:00:00Z"
                ),
                Instant.parse(
                        "2026-09-30T12:00:00Z"
                )
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );
    }

    @Test
    void shouldNotListProgramWithRevokedAccess()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Revoked Program",
                        null
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        access.revoke(
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                )
        );

        programAccessRepository.saveAndFlush(
                access
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );
    }

    @Test
    void shouldNotDuplicateProgramWhenMemberHasMultipleActiveAccesses()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-08-01T12:00:00Z"
                ),
                null
        );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].programId")
                                .value(
                                        program.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$[0].accessStartsAt")
                                .value(
                                        "2026-09-01T12:00:00Z"
                                )
                );
    }

    @Test
    void shouldNotListAnotherUsersPrograms()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        User otherMember =
                createUser(
                        "other@example.com"
                );

        Program program =
                createProgram(
                        "Owner Program",
                        null
                );

        createAccess(
                owner,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie otherMemberCookie =
                authenticatedCookie(
                        otherMember
                );

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(
                                        otherMemberCookie
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );
    }

    @Test
    void shouldRejectUnauthenticatedMemberProgramRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/me/programs")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturnProgramDetailsForAuthorizedMember()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Six Week Strength",
                        "Six-week strength program."
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                ),
                Instant.parse(
                        "2027-03-20T12:00:00Z"
                )
        );

        createResource(
                program,
                "Nutrition Guide",
                1
        );

        createWeek(
                program,
                "Week 1",
                1
        );

        Cookie memberCookie =
                authenticatedCookie(member);

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
                        jsonPath("$.name")
                                .value(
                                        "Six Week Strength"
                                )
                )
                .andExpect(
                        jsonPath("$.description")
                                .value(
                                        "Six-week strength program."
                                )
                )
                .andExpect(
                        jsonPath("$.accessStartsAt")
                                .value(
                                        "2026-09-20T12:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.accessExpiresAt")
                                .value(
                                        "2027-03-20T12:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.resources.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.resources[0].title")
                                .value(
                                        "Nutrition Guide"
                                )
                )
                .andExpect(
                        jsonPath("$.weeks.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.weeks[0].title")
                                .value(
                                        "Week 1"
                                )
                );
    }

    @Test
    void shouldReturnResourcesAndWeeksOrderedByPosition()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        createResource(
                program,
                "Resource 2",
                2
        );

        createResource(
                program,
                "Resource 1",
                1
        );

        createWeek(
                program,
                "Week 2",
                2
        );

        createWeek(
                program,
                "Week 1",
                1
        );

        Cookie memberCookie =
                authenticatedCookie(member);

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
                        jsonPath("$.resources[0].position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.resources[0].title")
                                .value(
                                        "Resource 1"
                                )
                )
                .andExpect(
                        jsonPath("$.resources[1].position")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.weeks[0].position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.weeks[0].title")
                                .value(
                                        "Week 1"
                                )
                )
                .andExpect(
                        jsonPath("$.weeks[1].position")
                                .value(2)
                );
    }

    @Test
    void shouldRejectMemberWithoutAccessToProgram()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        User otherMember =
                createUser(
                        "other@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        createAccess(
                owner,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie otherCookie =
                authenticatedCookie(
                        otherMember
                );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                program.getId()
                        )
                                .cookie(otherCookie)
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program access denied"
                                )
                );
    }

    @Test
    void shouldRejectProgramDetailWhenAccessHasNotStarted()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Future Program",
                        null
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-10-02T12:00:00Z"
                ),
                null
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                program.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void shouldRejectProgramDetailWhenAccessIsExpired()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Expired Program",
                        null
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-01-01T12:00:00Z"
                ),
                Instant.parse(
                        "2026-09-30T12:00:00Z"
                )
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                program.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void shouldRejectProgramDetailWhenAccessIsRevoked()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Revoked Program",
                        null
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        access.revoke(
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                )
        );

        programAccessRepository.saveAndFlush(
                access
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                program.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void shouldReturnOnlyContentFromRequestedProgram()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program programA =
                createProgram(
                        "Program A",
                        null
                );

        Program programB =
                createProgram(
                        "Program B",
                        null
                );

        createAccess(
                member,
                programA,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        createResource(
                programA,
                "Resource A",
                1
        );

        createWeek(
                programA,
                "Week A",
                1
        );

        createResource(
                programB,
                "Resource B",
                1
        );

        createWeek(
                programB,
                "Week B",
                1
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                programA.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.resources.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.resources[0].title")
                                .value(
                                        "Resource A"
                                )
                )
                .andExpect(
                        jsonPath("$.weeks.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.weeks[0].title")
                                .value(
                                        "Week A"
                                )
                );
    }

    @Test
    void shouldRejectUnauthenticatedProgramDetailRequest()
            throws Exception {

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                program.getId()
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturnProgramWeekForAuthorizedMember()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body Strength",
                        "Lower body workout."
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.weekId")
                                .value(
                                        week.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Week 1")
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.workouts.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.workouts[0].programWeekWorkoutId"
                        )
                                .value(
                                        association.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.workouts[0].name")
                                .value(
                                        "Lower Body Strength"
                                )
                )
                .andExpect(
                        jsonPath("$.workouts[0].description")
                                .value(
                                        "Lower body workout."
                                )
                )
                .andExpect(
                        jsonPath("$.workouts[0].position")
                                .value(1)
                );
    }

    @Test
    void shouldReturnWeekWorkoutsOrderedByPosition()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workoutTwo =
                createWorkout(
                        "Workout 2",
                        null
                );

        Workout workoutOne =
                createWorkout(
                        "Workout 1",
                        null
                );

        addWorkoutToWeek(
                week,
                workoutTwo,
                2
        );

        addWorkoutToWeek(
                week,
                workoutOne,
                1
        );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.workouts.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.workouts[0].name")
                                .value("Workout 1")
                )
                .andExpect(
                        jsonPath("$.workouts[0].position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.workouts[1].name")
                                .value("Workout 2")
                )
                .andExpect(
                        jsonPath("$.workouts[1].position")
                                .value(2)
                );
    }

    @Test
    void shouldReturnEmptyWorkoutListWhenWeekHasNoWorkouts()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.workouts.length()")
                                .value(0)
                );
    }

    @Test
    void shouldReturnNotFoundWhenWeekBelongsToAnotherProgram()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program programA =
                createProgram(
                        "Program A",
                        null
                );

        Program programB =
                createProgram(
                        "Program B",
                        null
                );

        ProgramWeek weekFromProgramB =
                createWeek(
                        programB,
                        "Week B",
                        1
                );

        createAccess(
                member,
                programA,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}",
                                programA.getId(),
                                weekFromProgramB.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week not found"
                                )
                );
    }

    @Test
    void shouldRejectAnotherMemberFromProgramWeek()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        User otherMember =
                createUser(
                        "other@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        createAccess(
                owner,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie otherCookie =
                authenticatedCookie(
                        otherMember
                );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .cookie(otherCookie)
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program access denied"
                                )
                );
    }

    @Test
    void shouldRejectProgramWeekWhenAccessIsRevoked()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        access.revoke(
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                )
        );

        programAccessRepository.saveAndFlush(
                access
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                                .cookie(memberCookie)
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void shouldRejectUnauthenticatedProgramWeekRequest()
            throws Exception {

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                week.getId()
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldReturnWorkoutDetailsForAuthorizedMember()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body Strength",
                        "Lower body workout."
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        "Keep your chest upright.",
                        "Dumbbell",
                        "https://example.com/goblet-squat"
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10-12",
                        new BigDecimal("25.00"),
                        90,
                        "Control the lowering phase.",
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie memberCookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{programWeekWorkoutId}",
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
                        jsonPath("$.programWeekWorkoutId")
                                .value(
                                        association.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.workoutId")
                                .value(
                                        workout.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.name")
                                .value(
                                        "Lower Body Strength"
                                )
                )
                .andExpect(
                        jsonPath("$.description")
                                .value(
                                        "Lower body workout."
                                )
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.exercises.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].workoutExerciseId"
                        )
                                .value(
                                        workoutExercise.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.exercises[0].exerciseId")
                                .value(
                                        exercise.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.exercises[0].name")
                                .value(
                                        "Goblet Squat"
                                )
                )
                .andExpect(
                        jsonPath("$.exercises[0].instructions")
                                .value(
                                        "Keep your chest upright."
                                )
                )
                .andExpect(
                        jsonPath("$.exercises[0].equipment")
                                .value(
                                        "Dumbbell"
                                )
                )
                .andExpect(
                        jsonPath("$.exercises[0].videoUrl")
                                .value(
                                        "https://example.com/goblet-squat"
                                )
                )
                .andExpect(
                        jsonPath("$.exercises[0].sets")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.exercises[0].reps")
                                .value("10-12")
                )
                .andExpect(
                        jsonPath("$.exercises[0].suggestedWeightLb")
                                .value(25.00)
                )
                .andExpect(
                        jsonPath("$.exercises[0].restSeconds")
                                .value(90)
                )
                .andExpect(
                        jsonPath("$.exercises[0].notes")
                                .value(
                                        "Control the lowering phase."
                                )
                )
                .andExpect(
                        jsonPath("$.exercises[0].position")
                                .value(1)
                );
    }

    @Test
    void shouldReturnWorkoutExercisesOrderedByPosition()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Full Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exerciseTwo =
                createExercise(
                        "Dumbbell Row",
                        null,
                        "Dumbbell",
                        null
                );

        Exercise exerciseOne =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        addExerciseToWorkout(
                workout,
                exerciseTwo,
                3,
                "12",
                null,
                60,
                null,
                2
        );

        addExerciseToWorkout(
                workout,
                exerciseOne,
                3,
                "10",
                null,
                90,
                null,
                1
        );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie cookie =
                authenticatedCookie(member);

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{programWeekWorkoutId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                                .cookie(cookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.exercises.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.exercises[0].name")
                                .value("Goblet Squat")
                )
                .andExpect(
                        jsonPath("$.exercises[0].position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.exercises[1].name")
                                .value("Dumbbell Row")
                )
                .andExpect(
                        jsonPath("$.exercises[1].position")
                                .value(2)
                );
    }

    @Test
    void shouldReturnEmptyExerciseListWhenWorkoutHasNoExercises()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Empty Workout",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.exercises.length()")
                                .value(0)
                );
    }

    @Test
    void shouldReturnNotFoundWhenWorkoutBelongsToAnotherWeek()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek weekOne =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        ProgramWeek weekTwo =
                createWeek(
                        program,
                        "Week 2",
                        2
                );

        Workout workout =
                createWorkout(
                        "Workout from Week 2",
                        null
                );

        ProgramWeekWorkout associationFromWeekTwo =
                addWorkoutToWeek(
                        weekTwo,
                        workout,
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                program.getId(),
                                weekOne.getId(),
                                associationFromWeekTwo.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week workout not found"
                                )
                );
    }

    @Test
    void shouldReturnNotFoundWhenWorkoutWeekBelongsToAnotherProgram()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program programA =
                createProgram(
                        "Program A",
                        null
                );

        Program programB =
                createProgram(
                        "Program B",
                        null
                );

        ProgramWeek weekB =
                createWeek(
                        programB,
                        "Week B",
                        1
                );

        Workout workout =
                createWorkout(
                        "Workout B",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        weekB,
                        workout,
                        1
                );

        createAccess(
                member,
                programA,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                programA.getId(),
                                weekB.getId(),
                                association.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week not found"
                                )
                );
    }

    @Test
    void shouldRejectAnotherMemberFromProgramWorkout()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        User otherMember =
                createUser(
                        "other@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Workout",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        createAccess(
                owner,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                otherMember
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program access denied"
                                )
                );
    }

    @Test
    void shouldRejectProgramWorkoutWhenAccessIsRevoked()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Workout",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        access.revoke(
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                )
        );

        programAccessRepository.saveAndFlush(
                access
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void shouldRejectUnauthenticatedProgramWorkoutRequest()
            throws Exception {

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Workout",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldNotListDraftProgramWithActiveAccess()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createDraftProgram(
                        "Draft Program",
                        null
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get("/api/me/programs")
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );
    }

    @Test
    void shouldRejectDraftProgramWithActiveAccess()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createDraftProgram(
                        "Draft Program",
                        null
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                program.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program access denied"
                                )
                );
    }

    @Test
    void shouldRejectAdminWithoutProgramAccess()
            throws Exception {

        User admin =
                createUser(
                        "admin@example.com",
                        UserRole.ADMIN
                );

        Program program =
                createProgram(
                        "Published Program",
                        null
                );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                program.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                admin
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program access denied"
                                )
                );
    }

    @Test
    void shouldExposeWeekUnlockStatusAndDate()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
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

        createWeek(
                program,
                "Week 3",
                3
        );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-10-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}",
                                program.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
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
                )
                .andExpect(
                        jsonPath("$.weeks[2].position")
                                .value(3)
                )
                .andExpect(
                        jsonPath("$.weeks[2].unlocked")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.weeks[2].unlocksAt")
                                .value(
                                        "2026-10-15T12:00:00Z"
                                )
                );
    }

    @Test
    void shouldRejectLockedProgramWeekBeforeUnlockDate()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek weekTwo =
                createWeek(
                        program,
                        "Week 2",
                        2
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-10-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                weekTwo.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week locked"
                                )
                )
                .andExpect(
                        jsonPath("$.unlocksAt")
                                .value(
                                        "2026-10-08T12:00:00Z"
                                )
                );
    }

    @Test
    void shouldAllowProgramWeekExactlyAtUnlockDate()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek weekTwo =
                createWeek(
                        program,
                        "Week 2",
                        2
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-24T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}",
                                program.getId(),
                                weekTwo.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.weekId")
                                .value(
                                        weekTwo.getId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldRejectWorkoutFromLockedProgramWeek()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek weekTwo =
                createWeek(
                        program,
                        "Week 2",
                        2
                );

        Workout workout =
                createWorkout(
                        "Workout 2",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        weekTwo,
                        workout,
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-10-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                program.getId(),
                                weekTwo.getId(),
                                association.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week locked"
                                )
                )
                .andExpect(
                        jsonPath("$.unlocksAt")
                                .value(
                                        "2026-10-08T12:00:00Z"
                                )
                );
    }

    @Test
    void shouldAllowWorkoutExactlyAtProgramWeekUnlockDate()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek weekTwo =
                createWeek(
                        program,
                        "Week 2",
                        2
                );

        Workout workout =
                createWorkout(
                        "Workout 2",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        weekTwo,
                        workout,
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-24T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                program.getId(),
                                weekTwo.getId(),
                                association.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.programWeekWorkoutId")
                                .value(
                                        association.getId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldReturnMemberExerciseWeightOverride()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        programAccessExerciseLoadRepository
                .saveAndFlush(
                        new ProgramAccessExerciseLoad(
                                access,
                                workoutExercise,
                                new BigDecimal("30.00")
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}/weeks/{weekId}/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].suggestedWeightLb"
                        )
                                .value(25.00)
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].currentWeightLb"
                        )
                                .value(30.00)
                );
    }

    @Test
    void shouldCreateMemberExerciseWeightOverride()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        mockMvc.perform(
                        put(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}"
                                        + "/exercises/{workoutExerciseId}/load",
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
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
                        jsonPath("$.workoutExerciseId")
                                .value(
                                        workoutExercise
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.suggestedWeightLb")
                                .value(25.00)
                )
                .andExpect(
                        jsonPath("$.currentWeightLb")
                                .value(30.00)
                );

        assertTrue(
                programAccessExerciseLoadRepository
                        .findByProgramAccessIdAndWorkoutExerciseId(
                                access.getId(),
                                workoutExercise.getId()
                        )
                        .isPresent()
        );
    }

    @Test
    void shouldRejectAnotherMemberFromUpdatingExerciseLoad()
            throws Exception {

        User owner =
                createUser(
                        "owner@example.com"
                );

        User otherMember =
                createUser(
                        "other@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        createAccess(
                owner,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        put(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}"
                                        + "/exercises/{workoutExerciseId}/load",
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(
                                        authenticatedCookie(
                                                otherMember
                                        )
                                )
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
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program access denied"
                                )
                );

        assertEquals(
                0,
                programAccessExerciseLoadRepository.count()
        );
    }

    @Test
    void shouldRejectUpdatingExerciseLoadFromLockedWeek()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek weekTwo =
                createWeek(
                        program,
                        "Week 2",
                        2
                );

        Workout workout =
                createWorkout(
                        "Lower Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        weekTwo,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-10-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        put(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}"
                                        + "/exercises/{workoutExerciseId}/load",
                                program.getId(),
                                weekTwo.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
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
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program week locked"
                                )
                );

        assertEquals(
                0,
                programAccessExerciseLoadRepository.count()
        );
    }

    @Test
    void shouldRejectExerciseLoadFromAnotherWorkout()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workoutA =
                createWorkout(
                        "Workout A",
                        null
                );

        Workout workoutB =
                createWorkout(
                        "Workout B",
                        null
                );

        ProgramWeekWorkout associationA =
                addWorkoutToWeek(
                        week,
                        workoutA,
                        1
                );

        addWorkoutToWeek(
                week,
                workoutB,
                2
        );

        Exercise exercise =
                createExercise(
                        "Dumbbell Row",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise exerciseFromWorkoutB =
                addExerciseToWorkout(
                        workoutB,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        put(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}"
                                        + "/exercises/{workoutExerciseId}/load",
                                program.getId(),
                                week.getId(),
                                associationA.getId(),
                                exerciseFromWorkoutB.getId()
                        )
                                .with(csrf())
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
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
                        status().isNotFound()
                );

        assertEquals(
                0,
                programAccessExerciseLoadRepository.count()
        );
    }

    @Test
    void shouldRejectUnauthenticatedExerciseLoadUpdate()
            throws Exception {

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        mockMvc.perform(
                        put(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}"
                                        + "/exercises/{workoutExerciseId}/load",
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
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
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldRejectUpdatingExerciseLoadWithRevokedAccess()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        access.revoke(
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                )
        );

        programAccessRepository.saveAndFlush(
                access
        );

        mockMvc.perform(
                        put(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}"
                                        + "/exercises/{workoutExerciseId}/load",
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(
                                        authenticatedCookie(
                                                member
                                        )
                                )
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
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program access denied"
                                )
                );

        assertEquals(
                0,
                programAccessExerciseLoadRepository.count()
        );
    }

    @Test
    void shouldRejectNegativeExerciseWeight()
            throws Exception {

        User member =
                createUser("member@example.com");

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        mockMvc.perform(
                        put(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}"
                                        + "/exercises/{workoutExerciseId}/load",
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(
                                        authenticatedCookie(member)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "weightLb": -5.00
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        assertEquals(
                0,
                programAccessExerciseLoadRepository.count()
        );
    }

    @Test
    void shouldKeepExerciseLoadsIsolatedBetweenMembers()
            throws Exception {

        User memberA =
                createUser(
                        "member-a@example.com"
                );

        User memberB =
                createUser(
                        "member-b@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        createAccess(
                memberA,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        createAccess(
                memberB,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        String path =
                "/api/me/programs/{programId}"
                        + "/weeks/{weekId}"
                        + "/workouts/{associationId}"
                        + "/exercises/{workoutExerciseId}/load";

        mockMvc.perform(
                        put(
                                path,
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(
                                        authenticatedCookie(memberA)
                                )
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
                );

        mockMvc.perform(
                        put(
                                path,
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(
                                        authenticatedCookie(memberB)
                                )
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
                );

        assertEquals(
                2,
                programAccessExerciseLoadRepository.count()
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
                                .cookie(
                                        authenticatedCookie(memberA)
                                )
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

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                association.getId()
                        )
                                .cookie(
                                        authenticatedCookie(memberB)
                                )
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
    }

    @Test
    void shouldUpdateExistingExerciseLoadWithoutDuplicating()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workout =
                createWorkout(
                        "Lower Body",
                        null
                );

        ProgramWeekWorkout association =
                addWorkoutToWeek(
                        week,
                        workout,
                        1
                );

        Exercise exercise =
                createExercise(
                        "Goblet Squat",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise workoutExercise =
                addExerciseToWorkout(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("25.00"),
                        90,
                        null,
                        1
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        Cookie cookie =
                authenticatedCookie(member);

        String updatePath =
                "/api/me/programs/{programId}"
                        + "/weeks/{weekId}"
                        + "/workouts/{associationId}"
                        + "/exercises/{workoutExerciseId}/load";

        mockMvc.perform(
                        put(
                                updatePath,
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(cookie)
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
                );

        mockMvc.perform(
                        put(
                                updatePath,
                                program.getId(),
                                week.getId(),
                                association.getId(),
                                workoutExercise.getId()
                        )
                                .with(csrf())
                                .cookie(cookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "weightLb": 35.00
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.currentWeightLb")
                                .value(35.00)
                );

        assertEquals(
                1,
                programAccessExerciseLoadRepository.count()
        );

        ProgramAccessExerciseLoad load =
                programAccessExerciseLoadRepository
                        .findByProgramAccessIdAndWorkoutExerciseId(
                                access.getId(),
                                workoutExercise.getId()
                        )
                        .orElseThrow();

        assertEquals(
                new BigDecimal("35.00"),
                load.getWeightLb()
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
                                .cookie(cookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].suggestedWeightLb"
                        )
                                .value(25.00)
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].currentWeightLb"
                        )
                                .value(35.00)
                );
    }

    @Test
    void shouldKeepIndependentLoadsWhenSameExerciseIsUsedInDifferentWorkouts()
            throws Exception {

        User member =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program",
                        null
                );

        ProgramWeek week =
                createWeek(
                        program,
                        "Week 1",
                        1
                );

        Workout workoutA =
                createWorkout(
                        "Workout A",
                        null
                );

        Workout workoutB =
                createWorkout(
                        "Workout B",
                        null
                );

        ProgramWeekWorkout associationA =
                addWorkoutToWeek(
                        week,
                        workoutA,
                        1
                );

        ProgramWeekWorkout associationB =
                addWorkoutToWeek(
                        week,
                        workoutB,
                        2
                );

        Exercise exercise =
                createExercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        WorkoutExercise workoutExerciseA =
                addExerciseToWorkout(
                        workoutA,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("40.00"),
                        90,
                        null,
                        1
                );

        WorkoutExercise workoutExerciseB =
                addExerciseToWorkout(
                        workoutB,
                        exercise,
                        4,
                        "8",
                        new BigDecimal("60.00"),
                        120,
                        null,
                        1
                );

        createAccess(
                member,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        Cookie cookie =
                authenticatedCookie(member);

        String updatePath =
                "/api/me/programs/{programId}"
                        + "/weeks/{weekId}"
                        + "/workouts/{associationId}"
                        + "/exercises/{workoutExerciseId}/load";

        mockMvc.perform(
                        put(
                                updatePath,
                                program.getId(),
                                week.getId(),
                                associationA.getId(),
                                workoutExerciseA.getId()
                        )
                                .with(csrf())
                                .cookie(cookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "weightLb": 45.00
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        put(
                                updatePath,
                                program.getId(),
                                week.getId(),
                                associationB.getId(),
                                workoutExerciseB.getId()
                        )
                                .with(csrf())
                                .cookie(cookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "weightLb": 70.00
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isOk()
                );

        assertEquals(
                2,
                programAccessExerciseLoadRepository.count()
        );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                associationA.getId()
                        )
                                .cookie(cookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].exerciseId"
                        )
                                .value(
                                        exercise.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].currentWeightLb"
                        )
                                .value(45.00)
                );

        mockMvc.perform(
                        get(
                                "/api/me/programs/{programId}"
                                        + "/weeks/{weekId}"
                                        + "/workouts/{associationId}",
                                program.getId(),
                                week.getId(),
                                associationB.getId()
                        )
                                .cookie(cookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].exerciseId"
                        )
                                .value(
                                        exercise.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath(
                                "$.exercises[0].currentWeightLb"
                        )
                                .value(70.00)
                );
    }

    private Exercise createExercise(
            String name,
            String instructions,
            String equipment,
            String videoUrl
    ) {
        return exerciseRepository.saveAndFlush(
                new Exercise(
                        name,
                        instructions,
                        equipment,
                        videoUrl
                )
        );
    }

    private Workout createWorkout(
            String name,
            String description
    ) {
        return workoutRepository.saveAndFlush(
                new Workout(
                        name,
                        description
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

    private ProgramResource createResource(
            Program program,
            String title,
            Integer position
    ) {
        return programResourceRepository.saveAndFlush(
                new ProgramResource(
                        program,
                        title,
                        null,
                        "https://example.com/"
                                + position,
                        position
                )
        );
    }

    private User createUser(
            String email
    ) {
        return createUser(
                email,
                UserRole.USER
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

    private Program createProgram(
            String name,
            String description
    ) {
        Program program =
                new Program(
                        name,
                        description
                );

        program.publish();

        return programRepository.saveAndFlush(
                program
        );
    }

    private ProgramAccess createAccess(
            User user,
            Program program,
            Instant startsAt,
            Instant expiresAt
    ) {
        return programAccessRepository.saveAndFlush(
                new ProgramAccess(
                        user,
                        program,
                        startsAt,
                        expiresAt
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

    private WorkoutExercise addExerciseToWorkout(
            Workout workout,
            Exercise exercise,
            Integer sets,
            String reps,
            BigDecimal suggestedWeightLb,
            Integer restSeconds,
            String notes,
            Integer position
    ) {
        return workoutExerciseRepository
                .saveAndFlush(
                        new WorkoutExercise(
                                workout,
                                exercise,
                                sets,
                                reps,
                                suggestedWeightLb,
                                restSeconds,
                                notes,
                                position
                        )
                );
    }

    private Program createDraftProgram(
            String name,
            String description
    ) {
        return programRepository.saveAndFlush(
                new Program(
                        name,
                        description
                )
        );
    }
}