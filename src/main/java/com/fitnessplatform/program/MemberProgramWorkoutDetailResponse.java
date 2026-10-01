package com.fitnessplatform.program;

import com.fitnessplatform.workout.Workout;
import com.fitnessplatform.workout.WorkoutExercise;

import java.util.List;
import java.util.UUID;

public record MemberProgramWorkoutDetailResponse(
        UUID programWeekWorkoutId,
        UUID workoutId,
        String name,
        String description,
        Integer position,
        List<MemberWorkoutExerciseResponse> exercises
) {

    public static MemberProgramWorkoutDetailResponse from(
            ProgramWeekWorkout association,
            List<WorkoutExercise> exercises
    ) {
        Workout workout =
                association.getWorkout();

        return new MemberProgramWorkoutDetailResponse(
                association.getId(),
                workout.getId(),
                workout.getName(),
                workout.getDescription(),
                association.getPosition(),
                exercises.stream()
                        .map(
                                MemberWorkoutExerciseResponse::from
                        )
                        .toList()
        );
    }
}
