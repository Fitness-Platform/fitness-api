package com.fitnessplatform.exercise;

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
class ExerciseServiceTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    private ExerciseService exerciseService;

    @BeforeEach
    void setUp() {
        exerciseService =
                new ExerciseService(exerciseRepository);
    }

    @Test
    void shouldCreateExercise() {
        ExerciseRequest request =
                new ExerciseRequest(
                        "  Bench Press  ",
                        "  Lower the bar with control.  ",
                        "  Barbell  ",
                        "  https://example.com/video  "
                );

        when(exerciseRepository.save(any(Exercise.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Exercise exercise =
                exerciseService.create(request);

        assertEquals(
                "Bench Press",
                exercise.getName()
        );

        assertEquals(
                "Lower the bar with control.",
                exercise.getInstructions()
        );

        assertEquals(
                "Barbell",
                exercise.getEquipment()
        );

        assertEquals(
                "https://example.com/video",
                exercise.getVideoUrl()
        );

        verify(exerciseRepository)
                .save(exercise);
    }

    @Test
    void shouldConvertBlankOptionalFieldsToNull() {
        ExerciseRequest request =
                new ExerciseRequest(
                        "Push Up",
                        "   ",
                        "",
                        null
                );

        when(exerciseRepository.save(any(Exercise.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Exercise exercise =
                exerciseService.create(request);

        assertNull(exercise.getInstructions());
        assertNull(exercise.getEquipment());
        assertNull(exercise.getVideoUrl());
    }

    @Test
    void shouldReturnExerciseById() {
        UUID exerciseId = UUID.randomUUID();

        Exercise exercise =
                new Exercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        when(exerciseRepository.findById(exerciseId))
                .thenReturn(Optional.of(exercise));

        Exercise result =
                exerciseService.findById(exerciseId);

        assertSame(exercise, result);
    }

    @Test
    void shouldThrowWhenExerciseDoesNotExist() {
        UUID exerciseId = UUID.randomUUID();

        when(exerciseRepository.findById(exerciseId))
                .thenReturn(Optional.empty());

        assertThrows(
                ExerciseNotFoundException.class,
                () -> exerciseService.findById(exerciseId)
        );
    }

    @Test
    void shouldReturnAllExercises() {
        Exercise first =
                new Exercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        Exercise second =
                new Exercise(
                        "Push Up",
                        null,
                        null,
                        null
                );

        when(exerciseRepository.findAll())
                .thenReturn(List.of(first, second));

        List<Exercise> result =
                exerciseService.findAll();

        assertEquals(2, result.size());
        assertSame(first, result.get(0));
        assertSame(second, result.get(1));
    }

    @Test
    void shouldUpdateExercise() {
        UUID exerciseId = UUID.randomUUID();

        Exercise exercise =
                new Exercise(
                        "Bench Press",
                        "Old instructions",
                        "Barbell",
                        null
                );

        when(exerciseRepository.findById(exerciseId))
                .thenReturn(Optional.of(exercise));

        ExerciseRequest request =
                new ExerciseRequest(
                        "Incline Bench Press",
                        "Updated instructions",
                        "Barbell",
                        "https://example.com/incline"
                );

        Exercise result =
                exerciseService.update(
                        exerciseId,
                        request
                );

        assertSame(exercise, result);
        assertEquals(
                "Incline Bench Press",
                exercise.getName()
        );
        assertEquals(
                "Updated instructions",
                exercise.getInstructions()
        );
        assertEquals(
                "https://example.com/incline",
                exercise.getVideoUrl()
        );
    }

    @Test
    void shouldDeleteExistingExercise() {
        UUID exerciseId = UUID.randomUUID();

        Exercise exercise =
                new Exercise(
                        "Bench Press",
                        null,
                        "Barbell",
                        null
                );

        when(exerciseRepository.findById(exerciseId))
                .thenReturn(Optional.of(exercise));

        exerciseService.delete(exerciseId);

        verify(exerciseRepository)
                .delete(exercise);
    }
}