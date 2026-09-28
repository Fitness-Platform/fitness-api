package com.fitnessplatform.workout;

import com.fitnessplatform.exercise.Exercise;
import com.fitnessplatform.exercise.ExerciseNotFoundException;
import com.fitnessplatform.exercise.ExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkoutExerciseService {

    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutRepository workoutRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutExerciseService(
            WorkoutExerciseRepository workoutExerciseRepository,
            WorkoutRepository workoutRepository,
            ExerciseRepository exerciseRepository
    ) {
        this.workoutExerciseRepository =
                workoutExerciseRepository;

        this.workoutRepository =
                workoutRepository;

        this.exerciseRepository =
                exerciseRepository;
    }

    @Transactional
    public WorkoutExercise create(
            UUID workoutId,
            WorkoutExerciseCreateRequest request
    ) {
        Workout workout =
                findWorkout(workoutId);

        Exercise exercise =
                findExercise(request.exerciseId());

        ensurePositionAvailable(
                workoutId,
                request.position()
        );

        WorkoutExercise workoutExercise =
                new WorkoutExercise(
                        workout,
                        exercise,
                        request.sets(),
                        request.reps().strip(),
                        request.suggestedWeightLb(),
                        request.restSeconds(),
                        normalizeNullable(request.notes()),
                        request.position()
                );

        return workoutExerciseRepository.save(
                workoutExercise
        );
    }

    @Transactional(readOnly = true)
    public List<WorkoutExercise> findAll(
            UUID workoutId
    ) {
        findWorkout(workoutId);

        return workoutExerciseRepository
                .findAllByWorkoutIdOrderByPositionAsc(
                        workoutId
                );
    }

    @Transactional(readOnly = true)
    public WorkoutExercise findById(
            UUID workoutId,
            UUID workoutExerciseId
    ) {
        findWorkout(workoutId);

        return workoutExerciseRepository
                .findByIdAndWorkoutId(
                        workoutExerciseId,
                        workoutId
                )
                .orElseThrow(
                        () ->
                                new WorkoutExerciseNotFoundException(
                                        workoutExerciseId
                                )
                );
    }

    @Transactional
    public WorkoutExercise update(
            UUID workoutId,
            UUID workoutExerciseId,
            WorkoutExerciseUpdateRequest request
    ) {
        findWorkout(workoutId);

        WorkoutExercise workoutExercise =
                workoutExerciseRepository
                        .findByIdAndWorkoutId(
                                workoutExerciseId,
                                workoutId
                        )
                        .orElseThrow(
                                () ->
                                        new WorkoutExerciseNotFoundException(
                                                workoutExerciseId
                                        )
                        );

        ensurePositionAvailableForUpdate(
                workoutId,
                workoutExerciseId,
                request.position()
        );

        workoutExercise.update(
                request.sets(),
                request.reps().strip(),
                request.suggestedWeightLb(),
                request.restSeconds(),
                normalizeNullable(request.notes()),
                request.position()
        );

        return workoutExercise;
    }

    @Transactional
    public void delete(
            UUID workoutId,
            UUID workoutExerciseId
    ) {
        findWorkout(workoutId);

        WorkoutExercise workoutExercise =
                workoutExerciseRepository
                        .findByIdAndWorkoutId(
                                workoutExerciseId,
                                workoutId
                        )
                        .orElseThrow(
                                () ->
                                        new WorkoutExerciseNotFoundException(
                                                workoutExerciseId
                                        )
                        );

        workoutExerciseRepository.delete(
                workoutExercise
        );
    }

    private Workout findWorkout(
            UUID workoutId
    ) {
        return workoutRepository
                .findById(workoutId)
                .orElseThrow(
                        () ->
                                new WorkoutNotFoundException(
                                        workoutId
                                )
                );
    }

    private Exercise findExercise(
            UUID exerciseId
    ) {
        return exerciseRepository
                .findById(exerciseId)
                .orElseThrow(
                        () ->
                                new ExerciseNotFoundException(
                                        exerciseId
                                )
                );
    }

    private void ensurePositionAvailable(
            UUID workoutId,
            Integer position
    ) {
        if (
                workoutExerciseRepository
                        .existsByWorkoutIdAndPosition(
                                workoutId,
                                position
                        )
        ) {
            throw new WorkoutExercisePositionConflictException(
                    workoutId,
                    position
            );
        }
    }

    private void ensurePositionAvailableForUpdate(
            UUID workoutId,
            UUID workoutExerciseId,
            Integer position
    ) {
        if (
                workoutExerciseRepository
                        .existsByWorkoutIdAndPositionAndIdNot(
                                workoutId,
                                position,
                                workoutExerciseId
                        )
        ) {
            throw new WorkoutExercisePositionConflictException(
                    workoutId,
                    position
            );
        }
    }

    private String normalizeNullable(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.strip();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}