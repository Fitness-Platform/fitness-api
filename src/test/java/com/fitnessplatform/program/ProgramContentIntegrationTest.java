package com.fitnessplatform.program;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.exercise.ExerciseRepository;
import com.fitnessplatform.workout.WorkoutExerciseRepository;
import com.fitnessplatform.workout.WorkoutRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.fitnessplatform.exercise.Exercise;
import com.fitnessplatform.workout.Workout;
import com.fitnessplatform.workout.WorkoutExercise;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import com.fitnessplatform.exercise.Exercise;
import com.fitnessplatform.exercise.ExerciseInUseException;
import com.fitnessplatform.exercise.ExerciseService;
import com.fitnessplatform.workout.Workout;
import com.fitnessplatform.workout.WorkoutExercise;
import com.fitnessplatform.workout.WorkoutInUseException;
import com.fitnessplatform.workout.WorkoutService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProgramContentIntegrationTest {

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ProgramWeekRepository programWeekRepository;

    @Autowired
    private ProgramWeekWorkoutRepository programWeekWorkoutRepository;

    @Autowired
    private ProgramResourceRepository programResourceRepository;

    @Autowired
    private WorkoutRepository workoutRepository;

    @Autowired
    private WorkoutExerciseRepository workoutExerciseRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private WorkoutService workoutService;

    @Autowired
    private ExerciseService exerciseService;

    @BeforeEach
    void cleanDatabase() {
        programWeekWorkoutRepository.deleteAll();
        programResourceRepository.deleteAll();
        programWeekRepository.deleteAll();
        programRepository.deleteAll();

        workoutExerciseRepository.deleteAll();
        workoutRepository.deleteAll();

        exerciseRepository.deleteAll();
    }

    @Test
    @Transactional
    void shouldPersistAndRetrieveCompleteProgramContentGraph() {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Six Week Strength",
                                "Strength-focused program."
                        )
                );

        ProgramWeek week =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 1",
                                "Foundation week.",
                                1
                        )
                );

        Exercise exercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Bench Press",
                                "Lower the bar under control.",
                                "Barbell",
                                "https://example.com/bench-press"
                        )
                );

        Workout workout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Upper Body Strength",
                                "Upper body strength session."
                        )
                );

        WorkoutExercise workoutExercise =
                workoutExerciseRepository.saveAndFlush(
                        new WorkoutExercise(
                                workout,
                                exercise,
                                4,
                                "8-10",
                                new BigDecimal("135.00"),
                                120,
                                "Controlled tempo.",
                                1
                        )
                );

        ProgramWeekWorkout programWeekWorkout =
                programWeekWorkoutRepository.saveAndFlush(
                        new ProgramWeekWorkout(
                                week,
                                workout,
                                1
                        )
                );

        ProgramResource resource =
                programResourceRepository.saveAndFlush(
                        new ProgramResource(
                                program,
                                "Nutrition Guide",
                                "Supporting nutrition material.",
                                "https://example.com/nutrition-guide.pdf",
                                1
                        )
                );


        entityManager.clear();

        List<ProgramWeek> persistedWeeks =
                programWeekRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                program.getId()
                        );

        assertEquals(
                1,
                persistedWeeks.size()
        );

        ProgramWeek persistedWeek =
                persistedWeeks.getFirst();

        assertEquals(
                week.getId(),
                persistedWeek.getId()
        );

        assertEquals(
                program.getId(),
                persistedWeek.getProgram().getId()
        );

        assertEquals(
                1,
                persistedWeek.getPosition()
        );

        List<ProgramWeekWorkout> persistedProgramWorkouts =
                programWeekWorkoutRepository
                        .findAllByProgramWeekIdOrderByPositionAsc(
                                week.getId()
                        );

        assertEquals(
                1,
                persistedProgramWorkouts.size()
        );

        ProgramWeekWorkout persistedProgramWorkout =
                persistedProgramWorkouts.getFirst();

        assertEquals(
                programWeekWorkout.getId(),
                persistedProgramWorkout.getId()
        );

        assertEquals(
                workout.getId(),
                persistedProgramWorkout
                        .getWorkout()
                        .getId()
        );

        assertEquals(
                1,
                persistedProgramWorkout.getPosition()
        );

        List<WorkoutExercise> persistedWorkoutExercises =
                workoutExerciseRepository
                        .findAllByWorkoutIdOrderByPositionAsc(
                                workout.getId()
                        );

        assertEquals(
                1,
                persistedWorkoutExercises.size()
        );

        WorkoutExercise persistedWorkoutExercise =
                persistedWorkoutExercises.getFirst();

        assertEquals(
                workoutExercise.getId(),
                persistedWorkoutExercise.getId()
        );

        assertEquals(
                exercise.getId(),
                persistedWorkoutExercise
                        .getExercise()
                        .getId()
        );

        assertEquals(
                "8-10",
                persistedWorkoutExercise.getReps()
        );

        assertEquals(
                0,
                new BigDecimal("135.00")
                        .compareTo(
                                persistedWorkoutExercise
                                        .getSuggestedWeightLb()
                        )
        );

        List<ProgramResource> persistedResources =
                programResourceRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                program.getId()
                        );

        assertEquals(
                1,
                persistedResources.size()
        );

        ProgramResource persistedResource =
                persistedResources.getFirst();

        assertEquals(
                resource.getId(),
                persistedResource.getId()
        );

        assertEquals(
                program.getId(),
                persistedResource
                        .getProgram()
                        .getId()
        );

        assertEquals(
                "Nutrition Guide",
                persistedResource.getTitle()
        );
    }

    @Test
    @Transactional
    void shouldKeepProgramContentIsolatedBetweenPrograms() {

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

        ProgramWeek weekB =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                programB,
                                "Week B",
                                null,
                                1
                        )
                );

        ProgramResource resourceA =
                programResourceRepository.saveAndFlush(
                        new ProgramResource(
                                programA,
                                "Guide A",
                                null,
                                "https://example.com/a",
                                1
                        )
                );

        ProgramResource resourceB =
                programResourceRepository.saveAndFlush(
                        new ProgramResource(
                                programB,
                                "Guide B",
                                null,
                                "https://example.com/b",
                                1
                        )
                );

        entityManager.clear();

        List<ProgramWeek> programAWeeks =
                programWeekRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                programA.getId()
                        );

        List<ProgramWeek> programBWeeks =
                programWeekRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                programB.getId()
                        );

        List<ProgramResource> programAResources =
                programResourceRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                programA.getId()
                        );

        List<ProgramResource> programBResources =
                programResourceRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                programB.getId()
                        );

        assertEquals(
                1,
                programAWeeks.size()
        );

        assertEquals(
                weekA.getId(),
                programAWeeks.getFirst().getId()
        );

        assertEquals(
                1,
                programBWeeks.size()
        );

        assertEquals(
                weekB.getId(),
                programBWeeks.getFirst().getId()
        );

        assertEquals(
                1,
                programAResources.size()
        );

        assertEquals(
                resourceA.getId(),
                programAResources.getFirst().getId()
        );

        assertEquals(
                1,
                programBResources.size()
        );

        assertEquals(
                resourceB.getId(),
                programBResources.getFirst().getId()
        );
    }

    @Test
    @Transactional
    void shouldRetrieveProgramContentOrderedByPosition() {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        ProgramWeek weekThree =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 3",
                                null,
                                3
                        )
                );

        ProgramWeek weekOne =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 1",
                                null,
                                1
                        )
                );

        ProgramWeek weekTwo =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                program,
                                "Week 2",
                                null,
                                2
                        )
                );

        ProgramResource resourceTwo =
                programResourceRepository.saveAndFlush(
                        new ProgramResource(
                                program,
                                "Checklist",
                                null,
                                "https://example.com/checklist",
                                2
                        )
                );

        ProgramResource resourceOne =
                programResourceRepository.saveAndFlush(
                        new ProgramResource(
                                program,
                                "Nutrition Guide",
                                null,
                                "https://example.com/nutrition",
                                1
                        )
                );

        Workout workoutTwo =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Lower Body",
                                null
                        )
                );

        Workout workoutOne =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Upper Body",
                                null
                        )
                );

        ProgramWeekWorkout weekWorkoutTwo =
                programWeekWorkoutRepository.saveAndFlush(
                        new ProgramWeekWorkout(
                                weekOne,
                                workoutTwo,
                                2
                        )
                );

        ProgramWeekWorkout weekWorkoutOne =
                programWeekWorkoutRepository.saveAndFlush(
                        new ProgramWeekWorkout(
                                weekOne,
                                workoutOne,
                                1
                        )
                );

        Exercise secondExercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Incline Dumbbell Press",
                                null,
                                "Dumbbells",
                                null
                        )
                );

        Exercise firstExercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Bench Press",
                                null,
                                "Barbell",
                                null
                        )
                );

        WorkoutExercise workoutExerciseTwo =
                workoutExerciseRepository.saveAndFlush(
                        new WorkoutExercise(
                                workoutOne,
                                secondExercise,
                                3,
                                "10-12",
                                null,
                                90,
                                null,
                                2
                        )
                );

        WorkoutExercise workoutExerciseOne =
                workoutExerciseRepository.saveAndFlush(
                        new WorkoutExercise(
                                workoutOne,
                                firstExercise,
                                4,
                                "8-10",
                                null,
                                120,
                                null,
                                1
                        )
                );

        entityManager.clear();

        List<ProgramWeek> weeks =
                programWeekRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                program.getId()
                        );

        assertEquals(
                List.of(
                        weekOne.getId(),
                        weekTwo.getId(),
                        weekThree.getId()
                ),
                weeks.stream()
                        .map(ProgramWeek::getId)
                        .toList()
        );

        List<ProgramResource> resources =
                programResourceRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                program.getId()
                        );

        assertEquals(
                List.of(
                        resourceOne.getId(),
                        resourceTwo.getId()
                ),
                resources.stream()
                        .map(ProgramResource::getId)
                        .toList()
        );

        List<ProgramWeekWorkout> weekWorkouts =
                programWeekWorkoutRepository
                        .findAllByProgramWeekIdOrderByPositionAsc(
                                weekOne.getId()
                        );

        assertEquals(
                List.of(
                        weekWorkoutOne.getId(),
                        weekWorkoutTwo.getId()
                ),
                weekWorkouts.stream()
                        .map(ProgramWeekWorkout::getId)
                        .toList()
        );

        List<WorkoutExercise> workoutExercises =
                workoutExerciseRepository
                        .findAllByWorkoutIdOrderByPositionAsc(
                                workoutOne.getId()
                        );

        assertEquals(
                List.of(
                        workoutExerciseOne.getId(),
                        workoutExerciseTwo.getId()
                ),
                workoutExercises.stream()
                        .map(WorkoutExercise::getId)
                        .toList()
        );
    }

    @Test
    @Transactional
    void shouldReuseSameWorkoutAcrossDifferentProgramWeeks() {

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

        ProgramWeek weekB =
                programWeekRepository.saveAndFlush(
                        new ProgramWeek(
                                programB,
                                "Week B",
                                null,
                                1
                        )
                );

        Workout sharedWorkout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Full Body Strength",
                                "Reusable workout template."
                        )
                );

        ProgramWeekWorkout associationA =
                programWeekWorkoutRepository.saveAndFlush(
                        new ProgramWeekWorkout(
                                weekA,
                                sharedWorkout,
                                1
                        )
                );

        ProgramWeekWorkout associationB =
                programWeekWorkoutRepository.saveAndFlush(
                        new ProgramWeekWorkout(
                                weekB,
                                sharedWorkout,
                                1
                        )
                );

        entityManager.clear();

        List<ProgramWeekWorkout> programAWorkouts =
                programWeekWorkoutRepository
                        .findAllByProgramWeekIdOrderByPositionAsc(
                                weekA.getId()
                        );

        List<ProgramWeekWorkout> programBWorkouts =
                programWeekWorkoutRepository
                        .findAllByProgramWeekIdOrderByPositionAsc(
                                weekB.getId()
                        );

        assertEquals(
                1,
                programAWorkouts.size()
        );

        assertEquals(
                1,
                programBWorkouts.size()
        );

        assertEquals(
                associationA.getId(),
                programAWorkouts.getFirst().getId()
        );

        assertEquals(
                associationB.getId(),
                programBWorkouts.getFirst().getId()
        );

        assertEquals(
                sharedWorkout.getId(),
                programAWorkouts
                        .getFirst()
                        .getWorkout()
                        .getId()
        );

        assertEquals(
                sharedWorkout.getId(),
                programBWorkouts
                        .getFirst()
                        .getWorkout()
                        .getId()
        );
    }

    @Test
    @Transactional
    void shouldDeleteProgramCompositionWithoutDeletingReusableTemplates() {

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

        Exercise exercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Bench Press",
                                "Controlled movement.",
                                "Barbell",
                                null
                        )
                );

        Workout workout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Upper Body",
                                null
                        )
                );

        WorkoutExercise workoutExercise =
                workoutExerciseRepository.saveAndFlush(
                        new WorkoutExercise(
                                workout,
                                exercise,
                                4,
                                "8-10",
                                new BigDecimal("135.00"),
                                120,
                                null,
                                1
                        )
                );

        ProgramWeekWorkout programWeekWorkout =
                programWeekWorkoutRepository.saveAndFlush(
                        new ProgramWeekWorkout(
                                week,
                                workout,
                                1
                        )
                );

        ProgramResource resource =
                programResourceRepository.saveAndFlush(
                        new ProgramResource(
                                program,
                                "Nutrition Guide",
                                null,
                                "https://example.com/nutrition",
                                1
                        )
                );

        UUID programId =
                program.getId();

        UUID weekId =
                week.getId();

        UUID programWeekWorkoutId =
                programWeekWorkout.getId();

        UUID resourceId =
                resource.getId();

        UUID workoutId =
                workout.getId();

        UUID workoutExerciseId =
                workoutExercise.getId();

        UUID exerciseId =
                exercise.getId();

        entityManager.clear();

        programRepository.deleteById(programId);
        programRepository.flush();

        entityManager.clear();

        assertFalse(
                programRepository.existsById(
                        programId
                )
        );

        assertFalse(
                programWeekRepository.existsById(
                        weekId
                )
        );

        assertFalse(
                programWeekWorkoutRepository.existsById(
                        programWeekWorkoutId
                )
        );

        assertFalse(
                programResourceRepository.existsById(
                        resourceId
                )
        );

        assertTrue(
                workoutRepository.existsById(
                        workoutId
                )
        );

        assertTrue(
                workoutExerciseRepository.existsById(
                        workoutExerciseId
                )
        );

        assertTrue(
                exerciseRepository.existsById(
                        exerciseId
                )
        );
    }

    @Test
    void shouldPreventDeletingWorkoutUsedByProgramWeek() {

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

        UUID workoutId =
                workout.getId();

        UUID associationId =
                association.getId();

        assertThrows(
                WorkoutInUseException.class,
                () -> workoutService.delete(
                        workoutId
                )
        );

        assertTrue(
                workoutRepository.existsById(
                        workoutId
                )
        );

        assertTrue(
                programWeekWorkoutRepository.existsById(
                        associationId
                )
        );
    }

    @Test
    void shouldPreventDeletingExerciseUsedByWorkout() {

        Workout workout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Upper Body",
                                null
                        )
                );

        Exercise exercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Bench Press",
                                "Controlled movement.",
                                "Barbell",
                                null
                        )
                );

        WorkoutExercise workoutExercise =
                workoutExerciseRepository.saveAndFlush(
                        new WorkoutExercise(
                                workout,
                                exercise,
                                4,
                                "8-10",
                                new BigDecimal("135.00"),
                                120,
                                null,
                                1
                        )
                );

        UUID exerciseId =
                exercise.getId();

        UUID workoutExerciseId =
                workoutExercise.getId();

        assertThrows(
                ExerciseInUseException.class,
                () -> exerciseService.delete(
                        exerciseId
                )
        );

        assertTrue(
                exerciseRepository.existsById(
                        exerciseId
                )
        );

        assertTrue(
                workoutExerciseRepository.existsById(
                        workoutExerciseId
                )
        );
    }

    @Test
    @Transactional
    void shouldReuseSameExerciseAcrossWorkoutsWithIndependentConfiguration() {

        Exercise exercise =
                exerciseRepository.saveAndFlush(
                        new Exercise(
                                "Bench Press",
                                "Lower the bar under control.",
                                "Barbell",
                                "https://example.com/bench-press"
                        )
                );

        Workout strengthWorkout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Strength Workout",
                                null
                        )
                );

        Workout hypertrophyWorkout =
                workoutRepository.saveAndFlush(
                        new Workout(
                                "Hypertrophy Workout",
                                null
                        )
                );

        WorkoutExercise strengthConfiguration =
                workoutExerciseRepository.saveAndFlush(
                        new WorkoutExercise(
                                strengthWorkout,
                                exercise,
                                4,
                                "8-10",
                                new BigDecimal("135.00"),
                                120,
                                "Strength focus.",
                                1
                        )
                );

        WorkoutExercise hypertrophyConfiguration =
                workoutExerciseRepository.saveAndFlush(
                        new WorkoutExercise(
                                hypertrophyWorkout,
                                exercise,
                                3,
                                "12",
                                new BigDecimal("95.00"),
                                60,
                                "Hypertrophy focus.",
                                1
                        )
                );

        UUID exerciseId = exercise.getId();

        entityManager.clear();

        List<WorkoutExercise> strengthExercises =
                workoutExerciseRepository
                        .findAllByWorkoutIdOrderByPositionAsc(
                                strengthWorkout.getId()
                        );

        List<WorkoutExercise> hypertrophyExercises =
                workoutExerciseRepository
                        .findAllByWorkoutIdOrderByPositionAsc(
                                hypertrophyWorkout.getId()
                        );

        assertEquals(
                1,
                strengthExercises.size()
        );

        assertEquals(
                1,
                hypertrophyExercises.size()
        );

        WorkoutExercise persistedStrength =
                strengthExercises.getFirst();

        WorkoutExercise persistedHypertrophy =
                hypertrophyExercises.getFirst();

        // Same reusable Exercise
        assertEquals(
                exerciseId,
                persistedStrength.getExercise().getId()
        );

        assertEquals(
                exerciseId,
                persistedHypertrophy.getExercise().getId()
        );

        // Different WorkoutExercise associations
        assertFalse(
                persistedStrength.getId()
                        .equals(persistedHypertrophy.getId())
        );

        // Strength configuration
        assertEquals(
                4,
                persistedStrength.getSets()
        );

        assertEquals(
                "8-10",
                persistedStrength.getReps()
        );

        assertEquals(
                0,
                new BigDecimal("135.00")
                        .compareTo(
                                persistedStrength.getSuggestedWeightLb()
                        )
        );

        // Hypertrophy configuration
        assertEquals(
                3,
                persistedHypertrophy.getSets()
        );

        assertEquals(
                "12",
                persistedHypertrophy.getReps()
        );

        assertEquals(
                0,
                new BigDecimal("95.00")
                        .compareTo(
                                persistedHypertrophy.getSuggestedWeightLb()
                        )
        );
    }
}