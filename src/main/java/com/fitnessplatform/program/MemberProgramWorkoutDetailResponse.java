package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccessWorkoutTrackingState;
import com.fitnessplatform.workout.Workout;
import com.fitnessplatform.workout.WorkoutExercise;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MemberProgramWorkoutDetailResponse(
        UUID programWeekWorkoutId,
        UUID workoutId,
        String name,
        String description,
        Integer position,
        Instant startedAt,
        Instant completedAt,
        List<MemberWorkoutExerciseResponse> exercises
) {

    public static MemberProgramWorkoutDetailResponse from(
            ProgramWeekWorkout association,
            List<WorkoutExercise> exercises,
            Map<UUID, BigDecimal> currentWeights,
            ProgramAccessWorkoutTrackingState trackingState
    ) {
        Workout workout =
                association.getWorkout();

        return new MemberProgramWorkoutDetailResponse(
                association.getId(),
                workout.getId(),
                workout.getName(),
                workout.getDescription(),
                association.getPosition(),
                trackingState.startedAt(),
                trackingState.completedAt(),
                exercises.stream()
                        .map(
                                workoutExercise ->
                                        MemberWorkoutExerciseResponse
                                                .from(
                                                        workoutExercise,
                                                        currentWeights.get(
                                                                workoutExercise
                                                                        .getId()
                                                        ),
                                                        trackingState.setsFor(
                                                                workoutExercise
                                                                        .getId()
                                                        )
                                                )
                        )
                        .toList()
        );
    }
}
