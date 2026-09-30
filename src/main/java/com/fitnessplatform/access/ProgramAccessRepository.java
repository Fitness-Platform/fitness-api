package com.fitnessplatform.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgramAccessRepository extends JpaRepository<ProgramAccess, UUID> {

    List<ProgramAccess>
    findAllByUserIdOrderByCreatedAtDesc(
            UUID userId
    );

    List<ProgramAccess>
    findAllByProgramIdOrderByCreatedAtDesc(
            UUID programId
    );

    List<ProgramAccess>
    findAllByUserIdAndProgramIdOrderByCreatedAtDesc(
            UUID userId,
            UUID programId
    );
}
