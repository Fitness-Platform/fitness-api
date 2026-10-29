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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from ProgramResource r where r.id = :id and r.program.id = :programId")
    Optional<ProgramResource> findLockedByIdAndProgramId(@org.springframework.data.repository.query.Param("id") UUID id, @org.springframework.data.repository.query.Param("programId") UUID programId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from ProgramResource r where r.program.id = :programId order by r.position")
    List<ProgramResource> findAllLockedByProgramId(@org.springframework.data.repository.query.Param("programId") UUID programId);
}