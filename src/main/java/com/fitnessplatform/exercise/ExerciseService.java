package com.fitnessplatform.exercise;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.fitnessplatform.media.*;
import java.util.UUID;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    private final MediaService mediaService;

    public ExerciseService(
            ExerciseRepository exerciseRepository,
            MediaService mediaService
    ) {
        this.exerciseRepository = exerciseRepository;
        this.mediaService = mediaService;
    }

    @Transactional
    public Exercise create(
            ExerciseRequest request
    ) {
        MediaService.validateSources(request.videoUrl(), request.videoMediaId(), false);
        MediaUpload media = mediaService.attach(request.videoMediaId(), MediaPurpose.EXERCISE_VIDEO, null);
        Exercise exercise = new Exercise(
                request.name().strip(),
                normalizeNullable(request.instructions()),
                normalizeNullable(request.equipment()),
                normalizeNullable(request.videoUrl())
        );

        exercise.setVideoMedia(media);
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
        Exercise exercise = exerciseRepository.findLockedById(exerciseId)
                .orElseThrow(() -> new ExerciseNotFoundException(exerciseId));
        MediaService.validateSources(request.videoUrl(), request.videoMediaId(), false);
        MediaUpload old = exercise.getVideoMedia();
        MediaUpload replacement = mediaService.attach(request.videoMediaId(), MediaPurpose.EXERCISE_VIDEO, old);

        exercise.update(
                request.name().strip(),
                normalizeNullable(request.instructions()),
                normalizeNullable(request.equipment()),
                normalizeNullable(request.videoUrl())
        );

        exercise.setVideoMedia(replacement);
        mediaService.retireReplaced(old, replacement);
        return exercise;
    }

    @Transactional
    public void delete(UUID exerciseId) {
        Exercise exercise = exerciseRepository.findLockedById(exerciseId)
                .orElseThrow(() -> new ExerciseNotFoundException(exerciseId));

        try {
            exerciseRepository.delete(exercise);
            exerciseRepository.flush();
            mediaService.retireReplaced(exercise.getVideoMedia(), null);

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
