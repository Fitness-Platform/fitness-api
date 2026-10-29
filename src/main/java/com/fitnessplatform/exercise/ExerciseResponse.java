package com.fitnessplatform.exercise;

import java.time.Instant;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        String name,
        String instructions,
        String equipment,
        String videoUrl,
        Instant createdAt,
        Instant updatedAt,
        com.fitnessplatform.media.MediaMetadataResponse uploadedMedia
) {

    public static ExerciseResponse from(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getName(),
                exercise.getInstructions(),
                exercise.getEquipment(),
                exercise.getVideoUrl(),
                exercise.getCreatedAt(),
                exercise.getUpdatedAt(),
                com.fitnessplatform.media.MediaMetadataResponse.from(exercise.getVideoMedia())
        );
    }
}
