package com.fitnessplatform.workout;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;

    public WorkoutService(
            WorkoutRepository workoutRepository
    ) {
        this.workoutRepository = workoutRepository;
    }

    @Transactional
    public Workout create(
            WorkoutRequest request
    ) {
        Workout workout = new Workout(
                request.name().strip(),
                normalizeNullable(request.description())
        );

        return workoutRepository.save(workout);
    }

    @Transactional(readOnly = true)
    public List<Workout> findAll() {
        return workoutRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Workout findById(UUID workoutId) {
        return workoutRepository.findById(workoutId)
                .orElseThrow(
                        () -> new WorkoutNotFoundException(workoutId)
                );
    }

    @Transactional
    public Workout update(
            UUID workoutId,
            WorkoutRequest request
    ) {
        Workout workout = findById(workoutId);

        workout.update(
                request.name().strip(),
                normalizeNullable(request.description())
        );

        return workout;
    }

    @Transactional
    public void delete(
            UUID workoutId
    ) {
        Workout workout = findById(workoutId);

        workoutRepository.delete(workout);
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
