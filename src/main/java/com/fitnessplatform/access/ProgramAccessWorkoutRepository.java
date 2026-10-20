package com.fitnessplatform.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramAccessWorkoutRepository
        extends JpaRepository<ProgramAccessWorkout, UUID> {

    Optional<ProgramAccessWorkout>
    findByProgramAccessIdAndProgramWeekWorkoutId(
            UUID programAccessId,
            UUID programWeekWorkoutId
    );

    boolean
    existsByProgramAccessIdAndProgramWeekWorkoutId(
            UUID programAccessId,
            UUID programWeekWorkoutId
    );

    List<ProgramAccessWorkout>
    findAllByProgramAccessIdOrderByStartedAtDesc(
            UUID programAccessId
    );
}