package com.fitnessplatform.access;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProgramAccessWorkoutTrackingState(
        Instant startedAt,
        Instant completedAt,
        Map<UUID, List<ProgramAccessWorkoutSet>>
        setsByWorkoutExerciseId
) {

    public static ProgramAccessWorkoutTrackingState notStarted() {
        return new ProgramAccessWorkoutTrackingState(
                null,
                null,
                Map.of()
        );
    }

    public List<ProgramAccessWorkoutSet> setsFor(
            UUID workoutExerciseId
    ) {
        return setsByWorkoutExerciseId
                .getOrDefault(
                        workoutExerciseId,
                        List.of()
                );
    }
}