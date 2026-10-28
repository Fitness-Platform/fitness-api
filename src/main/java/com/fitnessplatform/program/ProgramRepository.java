package com.fitnessplatform.program;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    List<Program> findByStatusOrderByCreatedAtDescIdAsc(ProgramStatus status);

    Optional<Program> findByIdAndStatus(UUID id, ProgramStatus status);
}
