package com.fitnessplatform.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramAccessExerciseLoadRepository
        extends JpaRepository<
        ProgramAccessExerciseLoad,
        UUID
        > {

    Optional<ProgramAccessExerciseLoad>
    findByProgramAccessIdAndWorkoutExerciseId(
            UUID programAccessId,
            UUID workoutExerciseId
    );

    List<ProgramAccessExerciseLoad>
    findAllByProgramAccessId(
            UUID programAccessId
    );

    List<ProgramAccessExerciseLoad>
    findAllByProgramAccessIdAndWorkoutExerciseIdIn(
            UUID programAccessId,
            Collection<UUID> workoutExerciseIds
    );
}