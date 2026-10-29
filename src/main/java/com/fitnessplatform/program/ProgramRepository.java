package com.fitnessplatform.program;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    List<Program> findByStatusOrderByCreatedAtDescIdAsc(ProgramStatus status);

    Optional<Program> findByIdAndStatus(UUID id, ProgramStatus status);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Program p where p.id = :id")
    Optional<Program> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
}
