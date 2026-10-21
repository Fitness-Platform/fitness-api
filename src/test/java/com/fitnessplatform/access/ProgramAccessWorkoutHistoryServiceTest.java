package com.fitnessplatform.access;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramWeekWorkout;
import com.fitnessplatform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProgramAccessWorkoutHistoryServiceTest {

    private ProgramAccessRepository
            programAccessRepository;

    private ProgramAccessWorkoutRepository
            programAccessWorkoutRepository;

    private ProgramAccessWorkoutSetRepository
            programAccessWorkoutSetRepository;

    private ProgramAccessWorkoutHistoryService service;

    @BeforeEach
    void setUp() {
        programAccessRepository =
                mock(
                        ProgramAccessRepository.class
                );

        programAccessWorkoutRepository =
                mock(
                        ProgramAccessWorkoutRepository.class
                );

        programAccessWorkoutSetRepository =
                mock(
                        ProgramAccessWorkoutSetRepository.class
                );

        service =
                new ProgramAccessWorkoutHistoryService(
                        programAccessRepository,
                        programAccessWorkoutRepository,
                        programAccessWorkoutSetRepository
                );
    }

    @Test
    void shouldReturnMostRecentPreviousCompletedWorkout() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        UUID currentAccessId =
                UUID.randomUUID();

        UUID previousAccessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        Instant currentStartsAt =
                Instant.parse(
                        "2026-10-01T12:00:00Z"
                );

        Instant currentCreatedAt =
                Instant.parse(
                        "2026-09-30T12:00:00Z"
                );

        User user =
                mock(User.class);

        Program program =
                mock(Program.class);

        ProgramAccess currentAccess =
                mock(ProgramAccess.class);

        ProgramAccess previousAccess =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        ProgramAccessWorkout previousWorkout =
                mock(ProgramAccessWorkout.class);

        when(user.getId())
                .thenReturn(userId);

        when(program.getId())
                .thenReturn(programId);

        when(currentAccess.getUser())
                .thenReturn(user);

        when(currentAccess.getProgram())
                .thenReturn(program);

        when(currentAccess.getId())
                .thenReturn(currentAccessId);

        when(currentAccess.getStartsAt())
                .thenReturn(currentStartsAt);

        when(currentAccess.getCreatedAt())
                .thenReturn(currentCreatedAt);

        when(previousAccess.getId())
                .thenReturn(previousAccessId);

        when(programWeekWorkout.getId())
                .thenReturn(
                        programWeekWorkoutId
                );

        when(
                programAccessRepository
                        .findPreviousAccesses(
                                userId,
                                programId,
                                currentAccessId,
                                currentStartsAt,
                                currentCreatedAt
                        )
        ).thenReturn(
                List.of(
                        previousAccess
                )
        );

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutIdAndCompletedAtIsNotNull(
                                previousAccessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.of(
                        previousWorkout
                )
        );

        Optional<ProgramAccessWorkout> result =
                service.findPreviousCompletedWorkout(
                        currentAccess,
                        programWeekWorkout
                );

        assertTrue(
                result.isPresent()
        );

        assertSame(
                previousWorkout,
                result.orElseThrow()
        );
    }

    @Test
    void shouldSkipPreviousAccessWithoutCompletedWorkout() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        UUID currentAccessId =
                UUID.randomUUID();

        UUID newestPreviousAccessId =
                UUID.randomUUID();

        UUID olderPreviousAccessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        Instant currentStartsAt =
                Instant.parse(
                        "2026-10-01T12:00:00Z"
                );

        Instant currentCreatedAt =
                Instant.parse(
                        "2026-09-30T12:00:00Z"
                );

        User user =
                mock(User.class);

        Program program =
                mock(Program.class);

        ProgramAccess currentAccess =
                mock(ProgramAccess.class);

        ProgramAccess newestPreviousAccess =
                mock(ProgramAccess.class);

        ProgramAccess olderPreviousAccess =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        ProgramAccessWorkout olderCompletedWorkout =
                mock(ProgramAccessWorkout.class);

        when(user.getId())
                .thenReturn(userId);

        when(program.getId())
                .thenReturn(programId);

        when(currentAccess.getUser())
                .thenReturn(user);

        when(currentAccess.getProgram())
                .thenReturn(program);

        when(currentAccess.getId())
                .thenReturn(currentAccessId);

        when(currentAccess.getStartsAt())
                .thenReturn(currentStartsAt);

        when(currentAccess.getCreatedAt())
                .thenReturn(currentCreatedAt);

        when(newestPreviousAccess.getId())
                .thenReturn(
                        newestPreviousAccessId
                );

        when(olderPreviousAccess.getId())
                .thenReturn(
                        olderPreviousAccessId
                );

        when(programWeekWorkout.getId())
                .thenReturn(
                        programWeekWorkoutId
                );

        when(
                programAccessRepository
                        .findPreviousAccesses(
                                userId,
                                programId,
                                currentAccessId,
                                currentStartsAt,
                                currentCreatedAt
                        )
        ).thenReturn(
                List.of(
                        newestPreviousAccess,
                        olderPreviousAccess
                )
        );

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutIdAndCompletedAtIsNotNull(
                                newestPreviousAccessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutIdAndCompletedAtIsNotNull(
                                olderPreviousAccessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.of(
                        olderCompletedWorkout
                )
        );

        Optional<ProgramAccessWorkout> result =
                service.findPreviousCompletedWorkout(
                        currentAccess,
                        programWeekWorkout
                );

        assertTrue(
                result.isPresent()
        );

        assertSame(
                olderCompletedWorkout,
                result.orElseThrow()
        );
    }

    @Test
    void shouldReturnEmptyWhenNoPreviousCompletedWorkoutExists() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        UUID currentAccessId =
                UUID.randomUUID();

        UUID previousAccessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        Instant currentStartsAt =
                Instant.parse(
                        "2026-10-01T12:00:00Z"
                );

        Instant currentCreatedAt =
                Instant.parse(
                        "2026-09-30T12:00:00Z"
                );

        User user =
                mock(User.class);

        Program program =
                mock(Program.class);

        ProgramAccess currentAccess =
                mock(ProgramAccess.class);

        ProgramAccess previousAccess =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        when(user.getId())
                .thenReturn(userId);

        when(program.getId())
                .thenReturn(programId);

        when(currentAccess.getUser())
                .thenReturn(user);

        when(currentAccess.getProgram())
                .thenReturn(program);

        when(currentAccess.getId())
                .thenReturn(currentAccessId);

        when(currentAccess.getStartsAt())
                .thenReturn(currentStartsAt);

        when(currentAccess.getCreatedAt())
                .thenReturn(currentCreatedAt);

        when(previousAccess.getId())
                .thenReturn(previousAccessId);

        when(programWeekWorkout.getId())
                .thenReturn(
                        programWeekWorkoutId
                );

        when(
                programAccessRepository
                        .findPreviousAccesses(
                                userId,
                                programId,
                                currentAccessId,
                                currentStartsAt,
                                currentCreatedAt
                        )
        ).thenReturn(
                List.of(
                        previousAccess
                )
        );

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutIdAndCompletedAtIsNotNull(
                                previousAccessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.empty()
        );

        Optional<ProgramAccessWorkout> result =
                service.findPreviousCompletedWorkout(
                        currentAccess,
                        programWeekWorkout
                );

        assertTrue(
                result.isEmpty()
        );
    }
}