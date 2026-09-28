package com.fitnessplatform.exercise;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(
            ExerciseRepository exerciseRepository
    ) {
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional
    public Exercise create(
            ExerciseRequest request
    ) {
        Exercise exercise = new Exercise(
                request.name().strip(),
                normalizeNullable(request.instructions()),
                normalizeNullable(request.equipment()),
                normalizeNullable(request.videoUrl())
        );

        return exerciseRepository.save(exercise);
    }

    @Transactional(readOnly = true)
    public List<Exercise> findAll() {
        return exerciseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Exercise findById(UUID exerciseId) {
        return exerciseRepository
                .findById(exerciseId)
                .orElseThrow(
                        () -> new ExerciseNotFoundException(exerciseId)
                );
    }

    @Transactional
    public Exercise update(
            UUID exerciseId,
            ExerciseRequest request
    ) {
        Exercise exercise = findById(exerciseId);

        exercise.update(
                request.name().strip(),
                normalizeNullable(request.instructions()),
                normalizeNullable(request.equipment()),
                normalizeNullable(request.videoUrl())
        );

        return exercise;
    }

    @Transactional
    public void delete(UUID exerciseId) {
        Exercise exercise = findById(exerciseId);

        try {
            exerciseRepository.delete(exercise);
            exerciseRepository.flush();

        } catch (DataIntegrityViolationException exception) {
            throw new ExerciseInUseException(
                    exerciseId
            );
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        return normalized.isEmpty()
                ? null
                :normalized;
    }
}
