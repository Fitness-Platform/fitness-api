package com.fitnessplatform.media;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface MediaUploadRepository extends JpaRepository<MediaUpload, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MediaUpload m where m.id = :id")
    Optional<MediaUpload> findLockedById(@Param("id") UUID id);

    @Query("select m.id from MediaUpload m where m.state = com.fitnessplatform.media.MediaUpload.State.RETIRED "
            + "or (m.state in (com.fitnessplatform.media.MediaUpload.State.PENDING, "
            + "com.fitnessplatform.media.MediaUpload.State.VERIFIED) and m.createdAt < :cutoff)")
    List<UUID> findCleanupCandidates(@Param("cutoff") Instant cutoff);
    @Query("select m.id from MediaUpload m where m.state = com.fitnessplatform.media.MediaUpload.State.ATTACHED "
            + "and m.stagingCleaned = false and m.uploadExpiresAt < :cutoff")
    List<UUID> findStagingCleanupCandidates(@Param("cutoff") Instant cutoff);
}

