package com.fitnessplatform.access;

import com.fitnessplatform.workout.WorkoutExercise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgramAccessExerciseLoadServiceTest {

    private ProgramAccessExerciseLoadRepository repository;

    private ProgramAccessExerciseLoadService service;

    @BeforeEach
    void setUp() {
        repository =
                mock(
                        ProgramAccessExerciseLoadRepository.class
                );

        service =
                new ProgramAccessExerciseLoadService(
                        repository
                );
    }

    @Test
    void shouldUseSuggestedWeightWhenOverrideDoesNotExist() {
        UUID accessId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        when(access.getId())
                .thenReturn(accessId);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(workoutExercise.getSuggestedWeightLb())
                .thenReturn(
                        new BigDecimal("25.00")
                );

        when(
                repository
                        .findAllByProgramAccessIdAndWorkoutExerciseIdIn(
                                accessId,
                                List.of(workoutExerciseId)
                        )
        ).thenReturn(
                List.of()
        );

        Map<UUID, BigDecimal> result =
                service.resolveCurrentWeights(
                        access,
                        List.of(workoutExercise)
                );

        assertEquals(
                new BigDecimal("25.00"),
                result.get(workoutExerciseId)
        );
    }

    @Test
    void shouldUseOverrideWhenItExists() {
        UUID accessId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        ProgramAccessExerciseLoad load =
                mock(ProgramAccessExerciseLoad.class);

        when(access.getId())
                .thenReturn(accessId);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(workoutExercise.getSuggestedWeightLb())
                .thenReturn(
                        new BigDecimal("25.00")
                );

        when(load.getWorkoutExercise())
                .thenReturn(workoutExercise);

        when(load.getWeightLb())
                .thenReturn(
                        new BigDecimal("30.00")
                );

        when(
                repository
                        .findAllByProgramAccessIdAndWorkoutExerciseIdIn(
                                accessId,
                                List.of(workoutExerciseId)
                        )
        ).thenReturn(
                List.of(load)
        );

        Map<UUID, BigDecimal> result =
                service.resolveCurrentWeights(
                        access,
                        List.of(workoutExercise)
                );

        assertEquals(
                new BigDecimal("30.00"),
                result.get(workoutExerciseId)
        );
    }

    @Test
    void shouldKeepOverrideWhenSuggestedWeightChanges() {
        UUID accessId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        ProgramAccessExerciseLoad load =
                mock(ProgramAccessExerciseLoad.class);

        when(access.getId())
                .thenReturn(accessId);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(workoutExercise.getSuggestedWeightLb())
                .thenReturn(
                        new BigDecimal("27.50")
                );

        when(load.getWorkoutExercise())
                .thenReturn(workoutExercise);

        when(load.getWeightLb())
                .thenReturn(
                        new BigDecimal("30.00")
                );

        when(
                repository
                        .findAllByProgramAccessIdAndWorkoutExerciseIdIn(
                                accessId,
                                List.of(workoutExerciseId)
                        )
        ).thenReturn(
                List.of(load)
        );

        Map<UUID, BigDecimal> result =
                service.resolveCurrentWeights(
                        access,
                        List.of(workoutExercise)
                );

        assertEquals(
                new BigDecimal("30.00"),
                result.get(workoutExerciseId)
        );
    }

    @Test
    void shouldReturnNullWhenNoSuggestedWeightOrOverrideExists() {
        UUID accessId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        when(access.getId())
                .thenReturn(accessId);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(workoutExercise.getSuggestedWeightLb())
                .thenReturn(null);

        when(
                repository
                        .findAllByProgramAccessIdAndWorkoutExerciseIdIn(
                                accessId,
                                List.of(workoutExerciseId)
                        )
        ).thenReturn(
                List.of()
        );

        Map<UUID, BigDecimal> result =
                service.resolveCurrentWeights(
                        access,
                        List.of(workoutExercise)
                );

        assertTrue(
                result.containsKey(
                        workoutExerciseId
                )
        );

        assertNull(
                result.get(
                        workoutExerciseId
                )
        );
    }

    @Test
    void shouldReturnEmptyMapWithoutQueryWhenWorkoutHasNoExercises() {
        ProgramAccess access =
                mock(ProgramAccess.class);

        Map<UUID, BigDecimal> result =
                service.resolveCurrentWeights(
                        access,
                        List.of()
                );

        assertTrue(
                result.isEmpty()
        );

        verifyNoInteractions(
                repository
        );
    }

    @Test
    void shouldCreateWeightOverrideWhenItDoesNotExist() {
        UUID accessId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        when(access.getId())
                .thenReturn(accessId);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(
                repository
                        .findByProgramAccessIdAndWorkoutExerciseId(
                                accessId,
                                workoutExerciseId
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                repository.save(
                        any(ProgramAccessExerciseLoad.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProgramAccessExerciseLoad result =
                service.setWeight(
                        access,
                        workoutExercise,
                        new BigDecimal("30.00")
                );

        assertEquals(
                new BigDecimal("30.00"),
                result.getWeightLb()
        );

        verify(repository)
                .save(
                        any(
                                ProgramAccessExerciseLoad.class
                        )
                );
    }

    @Test
    void shouldUpdateExistingWeightOverride() {
        UUID accessId =
                UUID.randomUUID();

        UUID workoutExerciseId =
                UUID.randomUUID();

        ProgramAccess access =
                mock(ProgramAccess.class);

        WorkoutExercise workoutExercise =
                mock(WorkoutExercise.class);

        ProgramAccessExerciseLoad existingLoad =
                new ProgramAccessExerciseLoad(
                        access,
                        workoutExercise,
                        new BigDecimal("25.00")
                );

        when(access.getId())
                .thenReturn(accessId);

        when(workoutExercise.getId())
                .thenReturn(workoutExerciseId);

        when(
                repository
                        .findByProgramAccessIdAndWorkoutExerciseId(
                                accessId,
                                workoutExerciseId
                        )
        ).thenReturn(
                Optional.of(existingLoad)
        );

        ProgramAccessExerciseLoad result =
                service.setWeight(
                        access,
                        workoutExercise,
                        new BigDecimal("35.00")
                );

        assertSame(
                existingLoad,
                result
        );

        assertEquals(
                new BigDecimal("35.00"),
                result.getWeightLb()
        );

        verify(
                repository,
                never()
        ).save(any());
    }
}