package com.fitnessplatform.access;

import com.fitnessplatform.program.ProgramWeekWorkout;
import com.fitnessplatform.workout.WorkoutExercise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProgramAccessWorkoutTrackingServiceTest {

    private ProgramAccessRepository
            programAccessRepository;

    private ProgramAccessWorkoutRepository
            programAccessWorkoutRepository;

    private ProgramAccessWorkoutSetRepository
            programAccessWorkoutSetRepository;

    private ProgramAccessWorkoutTrackingService service;

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
                new ProgramAccessWorkoutTrackingService(
                        programAccessRepository,
                        programAccessWorkoutRepository,
                        programAccessWorkoutSetRepository
                );
    }

    @Test
    void shouldCreateWorkoutExecutionAndSetWhenSavingFirstSet() {
        UUID accessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        UUID accessWorkoutId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        ProgramAccessWorkout savedAccessWorkout =
                mock(ProgramAccessWorkout.class);

        when(access.getId())
                .thenReturn(accessId);

        mockLockedAccess(
                access,
                accessId
        );

        when(programWeekWorkout.getId())
                .thenReturn(programWeekWorkoutId);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(workoutExercise.getSets())
                .thenReturn(3);

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                accessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                programAccessWorkoutRepository.save(
                        any(ProgramAccessWorkout.class)
                )
        ).thenReturn(
                savedAccessWorkout
        );

        when(savedAccessWorkout.getId())
                .thenReturn(accessWorkoutId);

        when(savedAccessWorkout.getCompletedAt())
                .thenReturn(null);

        when(
                programAccessWorkoutSetRepository
                        .findByProgramAccessWorkoutIdAndWorkoutExerciseIdAndSetNumber(
                                accessWorkoutId,
                                workoutExerciseId,
                                1
                        )
        ).thenReturn(
                Optional.empty()
        );

        ProgramAccessWorkoutSet savedSet =
                mock(ProgramAccessWorkoutSet.class);

        when(
                programAccessWorkoutSetRepository.save(
                        any(ProgramAccessWorkoutSet.class)
                )
        ).thenReturn(savedSet);

        ProgramAccessWorkoutSet result =
                service.saveSet(
                        access,
                        programWeekWorkout,
                        workoutExercise,
                        1,
                        10,
                        new BigDecimal("35.00")
                );

        assertSame(
                savedSet,
                result
        );

        verify(
                programAccessWorkoutRepository
        ).save(
                any(ProgramAccessWorkout.class)
        );

        verify(
                programAccessWorkoutSetRepository
        ).save(
                any(ProgramAccessWorkoutSet.class)
        );
    }

    @Test
    void shouldReuseExistingWorkoutExecution() {
        UUID accessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        UUID accessWorkoutId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        ProgramAccessWorkout accessWorkout =
                mock(ProgramAccessWorkout.class);

        when(access.getId())
                .thenReturn(accessId);

        mockLockedAccess(
                access,
                accessId
        );

        when(programWeekWorkout.getId())
                .thenReturn(programWeekWorkoutId);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(workoutExercise.getSets())
                .thenReturn(3);

        when(accessWorkout.getId())
                .thenReturn(accessWorkoutId);

        when(accessWorkout.getCompletedAt())
                .thenReturn(null);

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                accessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.of(accessWorkout)
        );

        when(
                programAccessWorkoutSetRepository
                        .findByProgramAccessWorkoutIdAndWorkoutExerciseIdAndSetNumber(
                                accessWorkoutId,
                                workoutExerciseId,
                                1
                        )
        ).thenReturn(
                Optional.empty()
        );

        ProgramAccessWorkoutSet savedSet =
                mock(ProgramAccessWorkoutSet.class);

        when(
                programAccessWorkoutSetRepository.save(
                        any(ProgramAccessWorkoutSet.class)
                )
        ).thenReturn(savedSet);

        service.saveSet(
                access,
                programWeekWorkout,
                workoutExercise,
                1,
                10,
                new BigDecimal("35.00")
        );

        verify(
                programAccessWorkoutRepository,
                never()
        ).save(
                any(ProgramAccessWorkout.class)
        );

        verify(
                programAccessWorkoutSetRepository
        ).save(
                any(ProgramAccessWorkoutSet.class)
        );
    }

    @Test
    void shouldUpdateExistingSetInsteadOfCreatingDuplicate() {
        UUID accessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        UUID accessWorkoutId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        ProgramAccessWorkout accessWorkout =
                mock(ProgramAccessWorkout.class);

        ProgramAccessWorkoutSet existingSet =
                mock(ProgramAccessWorkoutSet.class);

        when(access.getId())
                .thenReturn(accessId);

        mockLockedAccess(
                access,
                accessId
        );

        when(programWeekWorkout.getId())
                .thenReturn(programWeekWorkoutId);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(workoutExercise.getSets())
                .thenReturn(3);

        when(accessWorkout.getId())
                .thenReturn(accessWorkoutId);

        when(accessWorkout.getCompletedAt())
                .thenReturn(null);

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                accessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.of(accessWorkout)
        );

        when(
                programAccessWorkoutSetRepository
                        .findByProgramAccessWorkoutIdAndWorkoutExerciseIdAndSetNumber(
                                accessWorkoutId,
                                workoutExerciseId,
                                1
                        )
        ).thenReturn(
                Optional.of(existingSet)
        );

        ProgramAccessWorkoutSet result =
                service.saveSet(
                        access,
                        programWeekWorkout,
                        workoutExercise,
                        1,
                        8,
                        new BigDecimal("37.50")
                );

        assertSame(
                existingSet,
                result
        );

        verify(
                existingSet
        ).updatePerformance(
                8,
                new BigDecimal("37.50")
        );

        verify(
                programAccessWorkoutSetRepository,
                never()
        ).save(
                any(ProgramAccessWorkoutSet.class)
        );
    }

    @Test
    void shouldRejectSetNumberBelowOne() {
        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        when(workoutExercise.getSets())
                .thenReturn(3);

        assertThrows(
                InvalidProgramAccessWorkoutSetException.class,
                () ->
                        service.saveSet(
                                mock(ProgramAccess.class),
                                mock(ProgramWeekWorkout.class),
                                workoutExercise,
                                0,
                                10,
                                new BigDecimal("35.00")
                        )
        );

        verifyNoInteractions(
                programAccessWorkoutRepository,
                programAccessWorkoutSetRepository
        );
    }

    @Test
    void shouldRejectSetNumberAbovePrescribedSets() {
        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        when(workoutExercise.getSets())
                .thenReturn(3);

        assertThrows(
                InvalidProgramAccessWorkoutSetException.class,
                () ->
                        service.saveSet(
                                mock(ProgramAccess.class),
                                mock(ProgramWeekWorkout.class),
                                workoutExercise,
                                4,
                                10,
                                new BigDecimal("35.00")
                        )
        );

        verifyNoInteractions(
                programAccessWorkoutRepository,
                programAccessWorkoutSetRepository
        );
    }

    @Test
    void shouldRejectNegativeReps() {
        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        when(workoutExercise.getSets())
                .thenReturn(3);

        assertThrows(
                InvalidProgramAccessWorkoutSetException.class,
                () ->
                        service.saveSet(
                                mock(ProgramAccess.class),
                                mock(ProgramWeekWorkout.class),
                                workoutExercise,
                                1,
                                -1,
                                new BigDecimal("35.00")
                        )
        );

        verifyNoInteractions(
                programAccessWorkoutRepository,
                programAccessWorkoutSetRepository
        );
    }

    @Test
    void shouldRejectNegativeWeight() {
        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        when(workoutExercise.getSets())
                .thenReturn(3);

        assertThrows(
                InvalidProgramAccessWorkoutSetException.class,
                () ->
                        service.saveSet(
                                mock(ProgramAccess.class),
                                mock(ProgramWeekWorkout.class),
                                workoutExercise,
                                1,
                                10,
                                new BigDecimal("-1.00")
                        )
        );

        verifyNoInteractions(
                programAccessWorkoutRepository,
                programAccessWorkoutSetRepository
        );
    }

    @Test
    void shouldRejectSetChangesAfterWorkoutCompletion() {
        UUID accessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        ProgramAccessWorkout accessWorkout =
                mock(ProgramAccessWorkout.class);

        when(access.getId())
                .thenReturn(accessId);

        mockLockedAccess(
                access,
                accessId
        );

        when(programWeekWorkout.getId())
                .thenReturn(programWeekWorkoutId);

        when(workoutExercise.getSets())
                .thenReturn(3);

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                accessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.of(accessWorkout)
        );

        when(accessWorkout.getCompletedAt())
                .thenReturn(
                        Instant.parse(
                                "2026-10-20T12:00:00Z"
                        )
                );

        assertThrows(
                ProgramAccessWorkoutCompletedException.class,
                () ->
                        service.saveSet(
                                access,
                                programWeekWorkout,
                                workoutExercise,
                                1,
                                10,
                                new BigDecimal("35.00")
                        )
        );

        verifyNoInteractions(
                programAccessWorkoutSetRepository
        );
    }

    @Test
    void shouldCompleteWorkoutWhenAllPrescribedSetsExist() {
        UUID accessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        UUID accessWorkoutId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        ProgramAccessWorkout accessWorkout =
                mock(ProgramAccessWorkout.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        ProgramAccessWorkoutSet setOne =
                mock(ProgramAccessWorkoutSet.class);

        ProgramAccessWorkoutSet setTwo =
                mock(ProgramAccessWorkoutSet.class);

        ProgramAccessWorkoutSet setThree =
                mock(ProgramAccessWorkoutSet.class);

        when(access.getId())
                .thenReturn(accessId);

        mockLockedAccess(
                access,
                accessId
        );

        when(programWeekWorkout.getId())
                .thenReturn(programWeekWorkoutId);

        when(accessWorkout.getId())
                .thenReturn(accessWorkoutId);

        when(accessWorkout.getCompletedAt())
                .thenReturn(null);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(workoutExercise.getSets())
                .thenReturn(3);

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                accessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.of(accessWorkout)
        );

        when(
                programAccessWorkoutSetRepository
                        .findAllByProgramAccessWorkoutIdOrderByCompletedAtAsc(
                                accessWorkoutId
                        )
        ).thenReturn(
                List.of(
                        setOne,
                        setTwo,
                        setThree
                )
        );

        when(setOne.getWorkoutExercise())
                .thenReturn(workoutExercise);

        when(setTwo.getWorkoutExercise())
                .thenReturn(workoutExercise);

        when(setThree.getWorkoutExercise())
                .thenReturn(workoutExercise);

        when(setOne.getSetNumber())
                .thenReturn(1);

        when(setTwo.getSetNumber())
                .thenReturn(2);

        when(setThree.getSetNumber())
                .thenReturn(3);

        ProgramAccessWorkout result =
                service.completeWorkout(
                        access,
                        programWeekWorkout,
                        List.of(workoutExercise)
                );

        assertSame(
                accessWorkout,
                result
        );

        verify(accessWorkout)
                .complete(
                        any(Instant.class)
                );
    }

    @Test
    void shouldRejectWorkoutCompletionWhenPrescribedSetIsMissing() {
        UUID accessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        UUID accessWorkoutId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        ProgramAccessWorkout accessWorkout =
                mock(ProgramAccessWorkout.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        ProgramAccessWorkoutSet setOne =
                mock(ProgramAccessWorkoutSet.class);

        ProgramAccessWorkoutSet setTwo =
                mock(ProgramAccessWorkoutSet.class);

        when(access.getId())
                .thenReturn(accessId);

        mockLockedAccess(
                access,
                accessId
        );

        when(programWeekWorkout.getId())
                .thenReturn(programWeekWorkoutId);

        when(accessWorkout.getId())
                .thenReturn(accessWorkoutId);

        when(accessWorkout.getCompletedAt())
                .thenReturn(null);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(workoutExercise.getSets())
                .thenReturn(3);

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                accessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.of(accessWorkout)
        );

        when(
                programAccessWorkoutSetRepository
                        .findAllByProgramAccessWorkoutIdOrderByCompletedAtAsc(
                                accessWorkoutId
                        )
        ).thenReturn(
                List.of(
                        setOne,
                        setTwo
                )
        );

        when(setOne.getWorkoutExercise())
                .thenReturn(workoutExercise);

        when(setTwo.getWorkoutExercise())
                .thenReturn(workoutExercise);

        when(setOne.getSetNumber())
                .thenReturn(1);

        when(setTwo.getSetNumber())
                .thenReturn(2);

        assertThrows(
                ProgramAccessWorkoutIncompleteException.class,
                () ->
                        service.completeWorkout(
                                access,
                                programWeekWorkout,
                                List.of(workoutExercise)
                        )
        );

        verify(
                accessWorkout,
                never()
        ).complete(
                any(Instant.class)
        );
    }

    @Test
    void shouldRejectWorkoutCompletionWhenWorkoutWasNotStarted() {
        UUID accessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        when(access.getId())
                .thenReturn(accessId);

        mockLockedAccess(
                access,
                accessId
        );

        when(programWeekWorkout.getId())
                .thenReturn(programWeekWorkoutId);

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                accessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramAccessWorkoutIncompleteException.class,
                () ->
                        service.completeWorkout(
                                access,
                                programWeekWorkout,
                                List.of(workoutExercise)
                        )
        );

        verifyNoInteractions(
                programAccessWorkoutSetRepository
        );
    }

    @Test
    void shouldReturnAlreadyCompletedWorkoutWithoutCompletingAgain() {
        UUID accessId =
                UUID.randomUUID();

        UUID programWeekWorkoutId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        ProgramWeekWorkout programWeekWorkout =
                mock(ProgramWeekWorkout.class);

        ProgramAccessWorkout accessWorkout =
                mock(ProgramAccessWorkout.class);

        when(access.getId())
                .thenReturn(accessId);

        mockLockedAccess(
                access,
                accessId
        );

        when(programWeekWorkout.getId())
                .thenReturn(programWeekWorkoutId);

        when(
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                accessId,
                                programWeekWorkoutId
                        )
        ).thenReturn(
                Optional.of(accessWorkout)
        );

        when(accessWorkout.getCompletedAt())
                .thenReturn(
                        Instant.parse(
                                "2026-10-20T14:00:00Z"
                        )
                );

        ProgramAccessWorkout result =
                service.completeWorkout(
                        access,
                        programWeekWorkout,
                        List.of()
                );

        assertSame(
                accessWorkout,
                result
        );

        verify(
                accessWorkout,
                never()
        ).complete(
                any(Instant.class)
        );

        verifyNoInteractions(
                programAccessWorkoutSetRepository
        );
    }

    private void mockLockedAccess(
            ProgramAccess access,
            UUID accessId
    ) {
        when(
                programAccessRepository
                        .findByIdForUpdate(
                                accessId
                        )
        ).thenReturn(
                Optional.of(access)
        );
    }
}