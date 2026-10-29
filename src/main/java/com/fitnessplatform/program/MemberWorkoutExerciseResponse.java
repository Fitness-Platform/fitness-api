package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccessWorkoutSet;
import com.fitnessplatform.exercise.Exercise;
import com.fitnessplatform.workout.WorkoutExercise;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MemberWorkoutExerciseResponse(
        UUID workoutExerciseId,
        UUID exerciseId,
        String name,
        String instructions,
        String equipment,
        String videoUrl,
        Integer sets,
        String reps,
        BigDecimal suggestedWeightLb,
        BigDecimal currentWeightLb,
        Integer restSeconds,
        String notes,
        Integer position,
        List<MemberWorkoutSetResponse> completedSets,
        List<MemberWorkoutPreviousSetResponse> previousSets
) {

    public static MemberWorkoutExerciseResponse from(
            WorkoutExercise workoutExercise,
            BigDecimal currentWeightLb,
            List<ProgramAccessWorkoutSet> completedSets
    ) {
        return from(
                workoutExercise,
                currentWeightLb,
                completedSets,
                List.of()
        );
    }

    public static MemberWorkoutExerciseResponse from(
            WorkoutExercise workoutExercise,
            BigDecimal currentWeightLb,
            List<ProgramAccessWorkoutSet> completedSets,
            List<ProgramAccessWorkoutSet> previousSets
    ) {
        return from(workoutExercise, currentWeightLb, completedSets, previousSets,
                workoutExercise.getExercise().getVideoUrl());
    }

    public static MemberWorkoutExerciseResponse from(WorkoutExercise workoutExercise, BigDecimal currentWeightLb,
            List<ProgramAccessWorkoutSet> completedSets, List<ProgramAccessWorkoutSet> previousSets, String videoUrl) {
        Exercise exercise =
                workoutExercise.getExercise();

        return new MemberWorkoutExerciseResponse(
                workoutExercise.getId(),
                exercise.getId(),
                exercise.getName(),
                exercise.getInstructions(),
                exercise.getEquipment(),
                videoUrl,
                workoutExercise.getSets(),
                workoutExercise.getReps(),
                workoutExercise.getSuggestedWeightLb(),
                currentWeightLb,
                workoutExercise.getRestSeconds(),
                workoutExercise.getNotes(),
                workoutExercise.getPosition(),
                completedSets.stream()
                        .map(
                                MemberWorkoutSetResponse::from
                        )
                        .toList(),
                previousSets.stream()
                        .map(
                                MemberWorkoutPreviousSetResponse::from
                        )
                        .toList()
        );
    }
    @Override
    public String toString() { return "MemberWorkoutExerciseResponse[media URL redacted]"; }
}