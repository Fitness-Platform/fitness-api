package com.fitnessplatform.workout;

import com.fitnessplatform.exercise.Exercise;
import com.fitnessplatform.exercise.ExerciseNotFoundException;
import com.fitnessplatform.exercise.ExerciseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutExerciseServiceTest {

    @Mock
    private WorkoutExerciseRepository workoutExerciseRepository;

    @Mock
    private WorkoutRepository workoutRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    private WorkoutExerciseService workoutExerciseService;

    @BeforeEach
    void setUp() {
        workoutExerciseService =
                new WorkoutExerciseService(
                        workoutExerciseRepository,
                        workoutRepository,
                        exerciseRepository
                );
    }

    @Test
    void shouldAddExerciseToWorkout() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        Workout workout =
                new Workout(
                        "Upper Body Strength",
                        null
                );

        Exercise exercise =
                new Exercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        WorkoutExerciseCreateRequest request =
                new WorkoutExerciseCreateRequest(
                        exerciseId,
                        3,
                        "  8-10  ",
                        new BigDecimal("40.00"),
                        90,
                        "  Keep control of the movement.  ",
                        1
                );

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.of(workout));

        when(exerciseRepository.findById(exerciseId))
                .thenReturn(Optional.of(exercise));

        when(
                workoutExerciseRepository
                        .existsByWorkoutIdAndPosition(
                                workoutId,
                                1
                        )
        ).thenReturn(false);

        when(
                workoutExerciseRepository.save(
                        any(WorkoutExercise.class)
                )
        ).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        WorkoutExercise result =
                workoutExerciseService.create(
                        workoutId,
                        request
                );

        assertSame(
                workout,
                result.getWorkout()
        );

        assertSame(
                exercise,
                result.getExercise()
        );

        assertEquals(
                3,
                result.getSets()
        );

        assertEquals(
                "8-10",
                result.getReps()
        );

        assertEquals(
                new BigDecimal("40.00"),
                result.getSuggestedWeightLb()
        );

        assertEquals(
                90,
                result.getRestSeconds()
        );

        assertEquals(
                "Keep control of the movement.",
                result.getNotes()
        );

        assertEquals(
                1,
                result.getPosition()
        );

        verify(workoutExerciseRepository)
                .save(result);
    }

    @Test
    void shouldConvertBlankNotesToNull() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        Workout workout =
                new Workout("Upper Body", null);

        Exercise exercise =
                new Exercise(
                        "Push Up",
                        null,
                        null,
                        null
                );

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.of(workout));

        when(exerciseRepository.findById(exerciseId))
                .thenReturn(Optional.of(exercise));

        when(
                workoutExerciseRepository
                        .existsByWorkoutIdAndPosition(
                                workoutId,
                                1
                        )
        ).thenReturn(false);

        when(
                workoutExerciseRepository.save(
                        any(WorkoutExercise.class)
                )
        ).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        WorkoutExercise result =
                workoutExerciseService.create(
                        workoutId,
                        new WorkoutExerciseCreateRequest(
                                exerciseId,
                                3,
                                "12",
                                null,
                                60,
                                "   ",
                                1
                        )
                );

        assertNull(result.getNotes());
    }

    @Test
    void shouldRejectMissingWorkoutWhenAddingExercise() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.empty());

        WorkoutExerciseCreateRequest request =
                new WorkoutExerciseCreateRequest(
                        exerciseId,
                        3,
                        "10",
                        null,
                        60,
                        null,
                        1
                );

        assertThrows(
                WorkoutNotFoundException.class,
                () -> workoutExerciseService.create(
                        workoutId,
                        request
                )
        );

        verifyNoInteractions(exerciseRepository);

        verify(
                workoutExerciseRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldRejectMissingExerciseWhenAddingToWorkout() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        Workout workout =
                new Workout("Upper Body", null);

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.of(workout));

        when(exerciseRepository.findById(exerciseId))
                .thenReturn(Optional.empty());

        WorkoutExerciseCreateRequest request =
                new WorkoutExerciseCreateRequest(
                        exerciseId,
                        3,
                        "10",
                        null,
                        60,
                        null,
                        1
                );

        assertThrows(
                ExerciseNotFoundException.class,
                () -> workoutExerciseService.create(
                        workoutId,
                        request
                )
        );

        verify(
                workoutExerciseRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldRejectDuplicatePositionWhenAddingExercise() {
        UUID workoutId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        Workout workout =
                new Workout("Upper Body", null);

        Exercise exercise =
                new Exercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.of(workout));

        when(exerciseRepository.findById(exerciseId))
                .thenReturn(Optional.of(exercise));

        when(
                workoutExerciseRepository
                        .existsByWorkoutIdAndPosition(
                                workoutId,
                                1
                        )
        ).thenReturn(true);

        WorkoutExerciseCreateRequest request =
                new WorkoutExerciseCreateRequest(
                        exerciseId,
                        3,
                        "10",
                        null,
                        60,
                        null,
                        1
                );

        assertThrows(
                WorkoutExercisePositionConflictException.class,
                () -> workoutExerciseService.create(
                        workoutId,
                        request
                )
        );

        verify(
                workoutExerciseRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldReturnWorkoutExercisesInRepositoryOrder() {
        UUID workoutId = UUID.randomUUID();

        Workout workout =
                new Workout("Upper Body", null);

        Exercise firstExercise =
                new Exercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        Exercise secondExercise =
                new Exercise(
                        "Dumbbell Row",
                        null,
                        "Dumbbell",
                        null
                );

        WorkoutExercise first =
                new WorkoutExercise(
                        workout,
                        firstExercise,
                        3,
                        "10",
                        null,
                        60,
                        null,
                        1
                );

        WorkoutExercise second =
                new WorkoutExercise(
                        workout,
                        secondExercise,
                        3,
                        "12",
                        null,
                        60,
                        null,
                        2
                );

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.of(workout));

        when(
                workoutExerciseRepository
                        .findAllByWorkoutIdOrderByPositionAsc(
                                workoutId
                        )
        ).thenReturn(
                List.of(first, second)
        );

        List<WorkoutExercise> result =
                workoutExerciseService.findAll(workoutId);

        assertEquals(2, result.size());
        assertSame(first, result.get(0));
        assertSame(second, result.get(1));

        verify(workoutExerciseRepository)
                .findAllByWorkoutIdOrderByPositionAsc(
                        workoutId
                );
    }

    @Test
    void shouldRejectWorkoutExerciseFromDifferentWorkout() {
        UUID workoutId = UUID.randomUUID();
        UUID workoutExerciseId = UUID.randomUUID();

        Workout workout =
                new Workout("Workout A", null);

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.of(workout));

        when(
                workoutExerciseRepository
                        .findByIdAndWorkoutId(
                                workoutExerciseId,
                                workoutId
                        )
        ).thenReturn(Optional.empty());

        assertThrows(
                WorkoutExerciseNotFoundException.class,
                () -> workoutExerciseService.findById(
                        workoutId,
                        workoutExerciseId
                )
        );
    }

    @Test
    void shouldUpdateWorkoutExercise() {
        UUID workoutId = UUID.randomUUID();
        UUID workoutExerciseId = UUID.randomUUID();

        Workout workout =
                new Workout("Upper Body", null);

        Exercise exercise =
                new Exercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        WorkoutExercise workoutExercise =
                new WorkoutExercise(
                        workout,
                        exercise,
                        3,
                        "10",
                        new BigDecimal("40.00"),
                        90,
                        "Old notes",
                        1
                );

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.of(workout));

        when(
                workoutExerciseRepository
                        .findByIdAndWorkoutId(
                                workoutExerciseId,
                                workoutId
                        )
        ).thenReturn(
                Optional.of(workoutExercise)
        );

        when(
                workoutExerciseRepository
                        .existsByWorkoutIdAndPositionAndIdNot(
                                workoutId,
                                2,
                                workoutExerciseId
                        )
        ).thenReturn(false);

        WorkoutExerciseUpdateRequest request =
                new WorkoutExerciseUpdateRequest(
                        4,
                        "  6-8  ",
                        new BigDecimal("50.00"),
                        120,
                        "  Increase load gradually.  ",
                        2
                );

        WorkoutExercise result =
                workoutExerciseService.update(
                        workoutId,
                        workoutExerciseId,
                        request
                );

        assertSame(
                workoutExercise,
                result
        );

        assertEquals(4, result.getSets());
        assertEquals("6-8", result.getReps());

        assertEquals(
                new BigDecimal("50.00"),
                result.getSuggestedWeightLb()
        );

        assertEquals(
                120,
                result.getRestSeconds()
        );

        assertEquals(
                "Increase load gradually.",
                result.getNotes()
        );

        assertEquals(
                2,
                result.getPosition()
        );

        assertSame(
                exercise,
                result.getExercise()
        );
    }

    @Test
    void shouldRejectPositionConflictWhenUpdatingWorkoutExercise() {
        UUID workoutId = UUID.randomUUID();
        UUID workoutExerciseId = UUID.randomUUID();

        Workout workout =
                new Workout("Upper Body", null);

        Exercise exercise =
                new Exercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        WorkoutExercise workoutExercise =
                new WorkoutExercise(
                        workout,
                        exercise,
                        3,
                        "10",
                        null,
                        90,
                        null,
                        1
                );

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.of(workout));

        when(
                workoutExerciseRepository
                        .findByIdAndWorkoutId(
                                workoutExerciseId,
                                workoutId
                        )
        ).thenReturn(
                Optional.of(workoutExercise)
        );

        when(
                workoutExerciseRepository
                        .existsByWorkoutIdAndPositionAndIdNot(
                                workoutId,
                                2,
                                workoutExerciseId
                        )
        ).thenReturn(true);

        WorkoutExerciseUpdateRequest request =
                new WorkoutExerciseUpdateRequest(
                        4,
                        "8",
                        null,
                        90,
                        null,
                        2
                );

        assertThrows(
                WorkoutExercisePositionConflictException.class,
                () -> workoutExerciseService.update(
                        workoutId,
                        workoutExerciseId,
                        request
                )
        );

        assertEquals(
                1,
                workoutExercise.getPosition()
        );
    }

    @Test
    void shouldDeleteWorkoutExercise() {
        UUID workoutId = UUID.randomUUID();
        UUID workoutExerciseId = UUID.randomUUID();

        Workout workout =
                new Workout("Upper Body", null);

        Exercise exercise =
                new Exercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        WorkoutExercise workoutExercise =
                new WorkoutExercise(
                        workout,
                        exercise,
                        3,
                        "10",
                        null,
                        90,
                        null,
                        1
                );

        when(workoutRepository.findById(workoutId))
                .thenReturn(Optional.of(workout));

        when(
                workoutExerciseRepository
                        .findByIdAndWorkoutId(
                                workoutExerciseId,
                                workoutId
                        )
        ).thenReturn(
                Optional.of(workoutExercise)
        );

        workoutExerciseService.delete(
                workoutId,
                workoutExerciseId
        );

        verify(workoutExerciseRepository)
                .delete(workoutExercise);
    }
}