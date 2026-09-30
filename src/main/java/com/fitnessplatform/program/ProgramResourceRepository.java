package com.fitnessplatform.program;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramResourceRepository
        extends JpaRepository<ProgramResource, UUID> {

    List<ProgramResource>
    findAllByProgramIdOrderByPositionAsc(
            UUID programId
    );

    Optional<ProgramResource>
    findByIdAndProgramId(
            UUID id,
            UUID programId
    );

    boolean existsByProgramIdAndPosition(
            UUID programId,
            Integer position
    );

    boolean existsByProgramIdAndPositionAndIdNot(
            UUID programId,
            Integer position,
            UUID id
    );
}