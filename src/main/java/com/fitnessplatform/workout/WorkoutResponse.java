package com.fitnessplatform.workout;

import java.time.Instant;
import java.util.UUID;

public record WorkoutResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static WorkoutResponse from(
            Workout workout
    ) {
        return new WorkoutResponse(
                workout.getId(),
                workout.getName(),
                workout.getDescription(),
                workout.getCreatedAt(),
                workout.getUpdatedAt()
        );
    }
}
