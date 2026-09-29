package com.fitnessplatform.program;

import java.time.Instant;
import java.util.UUID;

public record ProgramWeekWorkoutResponse(
        UUID id,
        UUID programWeekId,
        UUID workoutId,
        Integer position,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProgramWeekWorkoutResponse from(ProgramWeekWorkout programWeekWorkout) {
        return new ProgramWeekWorkoutResponse(
                programWeekWorkout.getId(),
                programWeekWorkout.getProgramWeek().getId(),
                programWeekWorkout.getWorkout().getId(),
                programWeekWorkout.getPosition(),
                programWeekWorkout.getCreatedAt(),
                programWeekWorkout.getUpdatedAt()
        );
    }
}
