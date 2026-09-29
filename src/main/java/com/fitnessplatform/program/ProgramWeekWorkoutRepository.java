package com.fitnessplatform.program;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramWeekWorkoutRepository
        extends JpaRepository<ProgramWeekWorkout, UUID> {

    List<ProgramWeekWorkout>
    findAllByProgramWeekIdOrderByPositionAsc(
            UUID programWeekId
    );

    Optional<ProgramWeekWorkout>
    findByIdAndProgramWeekId(
            UUID id,
            UUID programWeekId
    );

    boolean existsByProgramWeekIdAndPosition(
            UUID programWeekId,
            Integer position
    );

    boolean existsByProgramWeekIdAndPositionAndIdNot(
            UUID programWeekId,
            Integer position,
            UUID id
    );
}