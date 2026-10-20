package com.fitnessplatform.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramAccessWorkoutSetRepository
        extends JpaRepository<ProgramAccessWorkoutSet, UUID> {

    Optional<ProgramAccessWorkoutSet>
    findByProgramAccessWorkoutIdAndWorkoutExerciseIdAndSetNumber(
            UUID programAccessWorkoutId,
            UUID workoutExerciseId,
            Integer setNumber
    );

    boolean
    existsByProgramAccessWorkoutIdAndWorkoutExerciseIdAndSetNumber(
            UUID programAccessWorkoutId,
            UUID workoutExerciseId,
            Integer setNumber
    );

    List<ProgramAccessWorkoutSet>
    findAllByProgramAccessWorkoutIdAndWorkoutExerciseIdOrderBySetNumberAsc(
            UUID programAccessWorkoutId,
            UUID workoutExerciseId
    );

    List<ProgramAccessWorkoutSet>
    findAllByProgramAccessWorkoutIdOrderByCompletedAtAsc(
            UUID programAccessWorkoutId
    );
}