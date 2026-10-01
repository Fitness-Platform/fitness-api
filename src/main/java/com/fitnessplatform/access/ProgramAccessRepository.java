package com.fitnessplatform.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProgramAccessRepository extends JpaRepository<ProgramAccess, UUID> {

    List<ProgramAccess>
    findAllByOrderByCreatedAtDesc();

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

    @Query("""
        SELECT access
        FROM ProgramAccess access
        WHERE access.user.id = :userId
          AND access.program.id = :programId
          AND access.startsAt <= :now
          AND access.revokedAt IS NULL
          AND (
                access.expiresAt IS NULL
                OR access.expiresAt > :now
          )
        ORDER BY access.startsAt DESC,
                 access.createdAt DESC
        """)
    List<ProgramAccess> findActiveAccesses(
            @Param("userId")
            UUID userId,

            @Param("programId")
            UUID programId,

            @Param("now")
            Instant now
    );

    @Query("""
        SELECT access
        FROM ProgramAccess access
        JOIN FETCH access.program
        WHERE access.user.id = :userId
          AND access.startsAt <= :now
          AND access.revokedAt IS NULL
          AND (
                access.expiresAt IS NULL
                OR access.expiresAt > :now
          )
        ORDER BY access.startsAt DESC,
                 access.createdAt DESC
        """)
    List<ProgramAccess> findActiveAccessesByUser(
            @Param("userId")
            UUID userId,

            @Param("now")
            Instant now
    );
}
