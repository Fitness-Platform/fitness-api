package com.fitnessplatform.access;

import com.fitnessplatform.program.ProgramWeekWorkout;
import com.fitnessplatform.workout.WorkoutExercise;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class ProgramAccessWorkoutTrackingService {

    private final ProgramAccessWorkoutRepository
            programAccessWorkoutRepository;

    private final ProgramAccessWorkoutSetRepository
            programAccessWorkoutSetRepository;

    private final ProgramAccessRepository
            programAccessRepository;

    public ProgramAccessWorkoutTrackingService(
            ProgramAccessRepository
                    programAccessRepository,
            ProgramAccessWorkoutRepository
                    programAccessWorkoutRepository,
            ProgramAccessWorkoutSetRepository
                    programAccessWorkoutSetRepository
    ) {
        this.programAccessRepository =
                programAccessRepository;

        this.programAccessWorkoutRepository =
                programAccessWorkoutRepository;

        this.programAccessWorkoutSetRepository =
                programAccessWorkoutSetRepository;
    }


    @Transactional
    public ProgramAccessWorkoutSet saveSet(
            ProgramAccess access,
            ProgramWeekWorkout programWeekWorkout,
            WorkoutExercise workoutExercise,
            Integer setNumber,
            Integer reps,
            BigDecimal weightLb
    ) {
        validateSet(
                workoutExercise,
                setNumber,
                reps,
                weightLb
        );

        ProgramAccess lockedAccess =
                programAccessRepository
                        .findByIdForUpdate(
                                access.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new ProgramAccessNotFoundException(
                                                access.getId()
                                        )
                        );

        ProgramAccessWorkout accessWorkout =
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                access.getId(),
                                programWeekWorkout.getId()
                        )
                        .orElseGet(
                                () ->
                                        programAccessWorkoutRepository
                                                .save(
                                                        new ProgramAccessWorkout(
                                                                lockedAccess,
                                                                programWeekWorkout,
                                                                Instant.now()
                                                        )
                                                )
                        );

        if (accessWorkout.getCompletedAt() != null) {
            throw new ProgramAccessWorkoutCompletedException();
        }

        return programAccessWorkoutSetRepository
                .findByProgramAccessWorkoutIdAndWorkoutExerciseIdAndSetNumber(
                        accessWorkout.getId(),
                        workoutExercise.getId(),
                        setNumber
                )
                .map(
                        existingSet -> {
                            existingSet.updatePerformance(
                                    reps,
                                    weightLb
                            );

                            return existingSet;
                        }
                )
                .orElseGet(
                        () ->
                                programAccessWorkoutSetRepository
                                        .save(
                                                new ProgramAccessWorkoutSet(
                                                        accessWorkout,
                                                        workoutExercise,
                                                        setNumber,
                                                        reps,
                                                        weightLb,
                                                        Instant.now()
                                                )
                                        )
                );
    }

    @Transactional
    public ProgramAccessWorkout completeWorkout(
            ProgramAccess access,
            ProgramWeekWorkout programWeekWorkout,
            List<WorkoutExercise> workoutExercises
    ) {

        ProgramAccess lockedAccess =
                programAccessRepository
                        .findByIdForUpdate(
                                access.getId()
                        )
                        .orElseThrow(
                                () ->
                                        new ProgramAccessNotFoundException(
                                                access.getId()
                                        )
                        );

        ProgramAccessWorkout accessWorkout =
                programAccessWorkoutRepository
                        .findByProgramAccessIdAndProgramWeekWorkoutId(
                                access.getId(),
                                programWeekWorkout.getId()
                        )
                        .orElseThrow(
                                ProgramAccessWorkoutIncompleteException::new
                        );

        if (accessWorkout.getCompletedAt() != null) {
            return accessWorkout;
        }

        List<ProgramAccessWorkoutSet> completedSets =
                programAccessWorkoutSetRepository
                        .findAllByProgramAccessWorkoutIdOrderByCompletedAtAsc(
                                accessWorkout.getId()
                        );

        validateWorkoutCompletion(
                workoutExercises,
                completedSets
        );

        accessWorkout.complete(
                Instant.now()
        );

        return accessWorkout;
    }

    private void validateWorkoutCompletion(
            List<WorkoutExercise> workoutExercises,
            List<ProgramAccessWorkoutSet> completedSets
    ) {
        Map<UUID, Set<Integer>> completedSetNumbersByExercise =
                new HashMap<>();

        for (
                ProgramAccessWorkoutSet completedSet
                : completedSets
        ) {
            UUID workoutExerciseId =
                    completedSet
                            .getWorkoutExercise()
                            .getId();

            completedSetNumbersByExercise
                    .computeIfAbsent(
                            workoutExerciseId,
                            ignored -> new HashSet<>()
                    )
                    .add(
                            completedSet.getSetNumber()
                    );
        }

        for (
                WorkoutExercise workoutExercise
                : workoutExercises
        ) {
            Set<Integer> completedSetNumbers =
                    completedSetNumbersByExercise
                            .getOrDefault(
                                    workoutExercise.getId(),
                                    Set.of()
                            );

            for (
                    int setNumber = 1;
                    setNumber <= workoutExercise.getSets();
                    setNumber++
            ) {
                if (
                        !completedSetNumbers.contains(
                                setNumber
                        )
                ) {
                    throw new ProgramAccessWorkoutIncompleteException();
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public ProgramAccessWorkoutTrackingState getTrackingState(
            ProgramAccess access,
            ProgramWeekWorkout programWeekWorkout
    ) {
        return programAccessWorkoutRepository
                .findByProgramAccessIdAndProgramWeekWorkoutId(
                        access.getId(),
                        programWeekWorkout.getId()
                )
                .map(
                        accessWorkout ->
                                buildTrackingState(
                                        accessWorkout
                                )
                )
                .orElseGet(
                        ProgramAccessWorkoutTrackingState::notStarted
                );
    }

    private ProgramAccessWorkoutTrackingState buildTrackingState(
            ProgramAccessWorkout accessWorkout
    ) {
        List<ProgramAccessWorkoutSet> sets =
                programAccessWorkoutSetRepository
                        .findAllByProgramAccessWorkoutIdOrderByCompletedAtAsc(
                                accessWorkout.getId()
                        );

        Map<UUID, List<ProgramAccessWorkoutSet>>
                setsByWorkoutExerciseId =
                sets.stream()
                        .collect(
                                Collectors.groupingBy(
                                        set ->
                                                set
                                                        .getWorkoutExercise()
                                                        .getId()
                                )
                        );

        setsByWorkoutExerciseId
                .values()
                .forEach(
                        exerciseSets ->
                                exerciseSets.sort(
                                        Comparator.comparing(
                                                ProgramAccessWorkoutSet::getSetNumber
                                        )
                                )
                );

        return new ProgramAccessWorkoutTrackingState(
                accessWorkout.getStartedAt(),
                accessWorkout.getCompletedAt(),
                setsByWorkoutExerciseId
        );
    }

    private void validateSet(
            WorkoutExercise workoutExercise,
            Integer setNumber,
            Integer reps,
            BigDecimal weightLb
    ) {
        if (
                setNumber == null
                        || setNumber < 1
                        || setNumber > workoutExercise.getSets()
        ) {
            throw new InvalidProgramAccessWorkoutSetException(
                    "Set number must be between 1 and "
                            + workoutExercise.getSets()
            );
        }

        if (
                reps == null
                        || reps < 0
        ) {
            throw new InvalidProgramAccessWorkoutSetException(
                    "Reps must be zero or greater"
            );
        }

        if (
                weightLb == null
                        || weightLb.signum() < 0
        ) {
            throw new InvalidProgramAccessWorkoutSetException(
                    "Weight must be zero or greater"
            );
        }
    }
}