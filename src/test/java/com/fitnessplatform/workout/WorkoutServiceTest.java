package com.fitnessplatform.workout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;

    private WorkoutService workoutService;

    @BeforeEach
    void setUp() {
        workoutService =
                new WorkoutService(
                        workoutRepository
                );
    }

    @Test
    void shouldCreateWorkout() {
        WorkoutRequest request =
                new WorkoutRequest(
                        "  Upper Body Strength  ",
                        "  Upper body workout.  "
                );

        when(workoutRepository.save(
                any(Workout.class)
        )).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        Workout workout =
                workoutService.create(request);

        assertEquals(
                "Upper Body Strength",
                workout.getName()
        );

        assertEquals(
                "Upper body workout.",
                workout.getDescription()
        );

        verify(workoutRepository)
                .save(workout);
    }

    @Test
    void shouldConvertBlankDescriptionToNull() {
        WorkoutRequest request =
                new WorkoutRequest(
                        "Lower Body Strength",
                        "   "
                );

        when(workoutRepository.save(
                any(Workout.class)
        )).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        Workout workout =
                workoutService.create(request);

        assertNull(
                workout.getDescription()
        );
    }

    @Test
    void shouldReturnWorkoutById() {
        UUID workoutId =
                UUID.randomUUID();

        Workout workout =
                new Workout(
                        "Upper Body Strength",
                        null
                );

        when(workoutRepository
                .findById(workoutId))
                .thenReturn(
                        Optional.of(workout)
                );

        Workout result =
                workoutService.findById(
                        workoutId
                );

        assertSame(
                workout,
                result
        );
    }

    @Test
    void shouldThrowWhenWorkoutDoesNotExist() {
        UUID workoutId =
                UUID.randomUUID();

        when(workoutRepository
                .findById(workoutId))
                .thenReturn(Optional.empty());

        assertThrows(
                WorkoutNotFoundException.class,
                () -> workoutService
                        .findById(workoutId)
        );
    }

    @Test
    void shouldReturnAllWorkouts() {
        Workout first =
                new Workout(
                        "Upper Body",
                        null
                );

        Workout second =
                new Workout(
                        "Lower Body",
                        null
                );

        when(workoutRepository.findAll())
                .thenReturn(
                        List.of(
                                first,
                                second
                        )
                );

        List<Workout> result =
                workoutService.findAll();

        assertEquals(
                2,
                result.size()
        );

        assertSame(
                first,
                result.get(0)
        );

        assertSame(
                second,
                result.get(1)
        );
    }

    @Test
    void shouldUpdateWorkout() {
        UUID workoutId =
                UUID.randomUUID();

        Workout workout =
                new Workout(
                        "Upper Body",
                        "Old description"
                );

        when(workoutRepository
                .findById(workoutId))
                .thenReturn(
                        Optional.of(workout)
                );

        WorkoutRequest request =
                new WorkoutRequest(
                        "Upper Body Strength",
                        "Updated description"
                );

        Workout result =
                workoutService.update(
                        workoutId,
                        request
                );

        assertSame(
                workout,
                result
        );

        assertEquals(
                "Upper Body Strength",
                workout.getName()
        );

        assertEquals(
                "Updated description",
                workout.getDescription()
        );
    }

    @Test
    void shouldDeleteExistingWorkout() {
        UUID workoutId =
                UUID.randomUUID();

        Workout workout =
                new Workout(
                        "Upper Body",
                        null
                );

        when(workoutRepository
                .findById(workoutId))
                .thenReturn(
                        Optional.of(workout)
                );

        workoutService.delete(
                workoutId
        );

        verify(workoutRepository)
                .delete(workout);
    }
}