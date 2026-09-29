package com.fitnessplatform.program;

import com.fitnessplatform.workout.Workout;
import com.fitnessplatform.workout.WorkoutNotFoundException;
import com.fitnessplatform.workout.WorkoutRepository;
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
class ProgramWeekWorkoutServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private ProgramWeekRepository programWeekRepository;

    @Mock
    private ProgramWeekWorkoutRepository programWeekWorkoutRepository;

    @Mock
    private WorkoutRepository workoutRepository;

    private ProgramWeekWorkoutService programWeekWorkoutService;

    @BeforeEach
    void setUp() {
        programWeekWorkoutService =
                new ProgramWeekWorkoutService(
                        programWeekWorkoutRepository,
                        programRepository,
                        programWeekRepository,
                        workoutRepository
                );
    }

    @Test
    void shouldAddWorkoutToProgramWeek() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID workoutId = UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        Workout workout =
                new Workout(
                        "Upper Body",
                        "Upper body workout"
                );

        ProgramWeekWorkoutCreateRequest request =
                new ProgramWeekWorkoutCreateRequest(
                        workoutId,
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                workoutRepository.findById(workoutId)
        ).thenReturn(
                Optional.of(workout)
        );

        when(
                programWeekWorkoutRepository
                        .existsByProgramWeekIdAndPosition(
                                programWeekId,
                                1
                        )
        ).thenReturn(false);

        when(
                programWeekWorkoutRepository.save(
                        any(ProgramWeekWorkout.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProgramWeekWorkout result =
                programWeekWorkoutService.create(
                        programId,
                        programWeekId,
                        request
                );

        assertSame(
                programWeek,
                result.getProgramWeek()
        );

        assertSame(
                workout,
                result.getWorkout()
        );

        assertEquals(
                1,
                result.getPosition()
        );

        verify(programWeekWorkoutRepository)
                .save(result);
    }

    @Test
    void shouldRejectMissingProgramOnCreate() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID workoutId = UUID.randomUUID();

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramNotFoundException.class,
                () ->
                        programWeekWorkoutService.create(
                                programId,
                                programWeekId,
                                new ProgramWeekWorkoutCreateRequest(
                                        workoutId,
                                        1
                                )
                        )
        );

        verifyNoInteractions(
                programWeekRepository,
                workoutRepository,
                programWeekWorkoutRepository
        );
    }

    @Test
    void shouldRejectProgramWeekFromDifferentProgramOnCreate() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID workoutId = UUID.randomUUID();

        Program program =
                new Program(
                        "Program B",
                        null
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramWeekNotFoundException.class,
                () ->
                        programWeekWorkoutService.create(
                                programId,
                                programWeekId,
                                new ProgramWeekWorkoutCreateRequest(
                                        workoutId,
                                        1
                                )
                        )
        );

        verifyNoInteractions(
                workoutRepository,
                programWeekWorkoutRepository
        );
    }

    @Test
    void shouldRejectMissingWorkoutOnCreate() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID workoutId = UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                workoutRepository.findById(workoutId)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                WorkoutNotFoundException.class,
                () ->
                        programWeekWorkoutService.create(
                                programId,
                                programWeekId,
                                new ProgramWeekWorkoutCreateRequest(
                                        workoutId,
                                        1
                                )
                        )
        );

        verify(
                programWeekWorkoutRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldRejectDuplicatePositionOnCreate() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID workoutId = UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        Workout workout =
                new Workout(
                        "Upper Body",
                        null
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                workoutRepository.findById(workoutId)
        ).thenReturn(
                Optional.of(workout)
        );

        when(
                programWeekWorkoutRepository
                        .existsByProgramWeekIdAndPosition(
                                programWeekId,
                                1
                        )
        ).thenReturn(true);

        assertThrows(
                ProgramWeekWorkoutPositionConflictException.class,
                () ->
                        programWeekWorkoutService.create(
                                programId,
                                programWeekId,
                                new ProgramWeekWorkoutCreateRequest(
                                        workoutId,
                                        1
                                )
                        )
        );

        verify(
                programWeekWorkoutRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldReturnWorkoutsOrderedByPosition() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        ProgramWeekWorkout first =
                new ProgramWeekWorkout(
                        programWeek,
                        new Workout(
                                "Upper Body",
                                null
                        ),
                        1
                );

        ProgramWeekWorkout second =
                new ProgramWeekWorkout(
                        programWeek,
                        new Workout(
                                "Lower Body",
                                null
                        ),
                        2
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                programWeekWorkoutRepository
                        .findAllByProgramWeekIdOrderByPositionAsc(
                                programWeekId
                        )
        ).thenReturn(
                List.of(first, second)
        );

        List<ProgramWeekWorkout> result =
                programWeekWorkoutService.findAll(
                        programId,
                        programWeekId
                );

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
    void shouldRejectAssociationFromDifferentProgramWeek() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID associationId = UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                programWeekWorkoutRepository
                        .findByIdAndProgramWeekId(
                                associationId,
                                programWeekId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramWeekWorkoutNotFoundException.class,
                () ->
                        programWeekWorkoutService.findById(
                                programId,
                                programWeekId,
                                associationId
                        )
        );
    }

    @Test
    void shouldUpdatePositionWithoutChangingWorkout() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID associationId = UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        Workout workout =
                new Workout(
                        "Upper Body",
                        null
                );

        ProgramWeekWorkout association =
                new ProgramWeekWorkout(
                        programWeek,
                        workout,
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                programWeekWorkoutRepository
                        .findByIdAndProgramWeekId(
                                associationId,
                                programWeekId
                        )
        ).thenReturn(
                Optional.of(association)
        );

        when(
                programWeekWorkoutRepository
                        .existsByProgramWeekIdAndPositionAndIdNot(
                                programWeekId,
                                2,
                                associationId
                        )
        ).thenReturn(false);

        ProgramWeekWorkout result =
                programWeekWorkoutService.update(
                        programId,
                        programWeekId,
                        associationId,
                        new ProgramWeekWorkoutUpdateRequest(
                                2
                        )
                );

        assertEquals(
                2,
                result.getPosition()
        );

        assertSame(
                workout,
                result.getWorkout()
        );
    }

    @Test
    void shouldAllowAssociationToKeepItsOwnPosition() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID associationId = UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        ProgramWeekWorkout association =
                new ProgramWeekWorkout(
                        programWeek,
                        new Workout(
                                "Upper Body",
                                null
                        ),
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                programWeekWorkoutRepository
                        .findByIdAndProgramWeekId(
                                associationId,
                                programWeekId
                        )
        ).thenReturn(
                Optional.of(association)
        );

        when(
                programWeekWorkoutRepository
                        .existsByProgramWeekIdAndPositionAndIdNot(
                                programWeekId,
                                1,
                                associationId
                        )
        ).thenReturn(false);

        ProgramWeekWorkout result =
                programWeekWorkoutService.update(
                        programId,
                        programWeekId,
                        associationId,
                        new ProgramWeekWorkoutUpdateRequest(
                                1
                        )
                );

        assertEquals(
                1,
                result.getPosition()
        );
    }

    @Test
    void shouldRejectDuplicatePositionOnUpdate() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID associationId = UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        ProgramWeekWorkout association =
                new ProgramWeekWorkout(
                        programWeek,
                        new Workout(
                                "Upper Body",
                                null
                        ),
                        2
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                programWeekWorkoutRepository
                        .findByIdAndProgramWeekId(
                                associationId,
                                programWeekId
                        )
        ).thenReturn(
                Optional.of(association)
        );

        when(
                programWeekWorkoutRepository
                        .existsByProgramWeekIdAndPositionAndIdNot(
                                programWeekId,
                                1,
                                associationId
                        )
        ).thenReturn(true);

        assertThrows(
                ProgramWeekWorkoutPositionConflictException.class,
                () ->
                        programWeekWorkoutService.update(
                                programId,
                                programWeekId,
                                associationId,
                                new ProgramWeekWorkoutUpdateRequest(
                                        1
                                )
                        )
        );

        assertEquals(
                2,
                association.getPosition()
        );
    }

    @Test
    void shouldDeleteProgramWeekWorkout() {
        UUID programId = UUID.randomUUID();
        UUID programWeekId = UUID.randomUUID();
        UUID associationId = UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        ProgramWeekWorkout association =
                new ProgramWeekWorkout(
                        programWeek,
                        new Workout(
                                "Upper Body",
                                null
                        ),
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository.findByIdAndProgramId(
                        programWeekId,
                        programId
                )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                programWeekWorkoutRepository
                        .findByIdAndProgramWeekId(
                                associationId,
                                programWeekId
                        )
        ).thenReturn(
                Optional.of(association)
        );

        programWeekWorkoutService.delete(
                programId,
                programWeekId,
                associationId
        );

        verify(programWeekWorkoutRepository)
                .delete(association);
    }
}