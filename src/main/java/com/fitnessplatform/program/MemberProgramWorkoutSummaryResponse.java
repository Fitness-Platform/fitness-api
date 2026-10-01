package com.fitnessplatform.program;

import com.fitnessplatform.workout.Workout;

import java.util.UUID;

public record MemberProgramWorkoutSummaryResponse(
        UUID programWeekWorkoutId,
        String name,
        String description,
        Integer position
) {

    public static MemberProgramWorkoutSummaryResponse from(
            ProgramWeekWorkout association
    ) {
        Workout workout =
                association.getWorkout();

        return new MemberProgramWorkoutSummaryResponse(
                association.getId(),
                workout.getName(),
                workout.getDescription(),
                association.getPosition()
        );
    }
}
