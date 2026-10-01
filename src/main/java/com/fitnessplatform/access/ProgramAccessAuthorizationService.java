package com.fitnessplatform.access;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProgramAccessAuthorizationService {

    private final ProgramAccessRepository programAccessRepository;

    private final Clock clock;

    public ProgramAccessAuthorizationService(
            ProgramAccessRepository programAccessRepository,
            Clock clock
    ) {
        this.programAccessRepository = programAccessRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<ProgramAccess> findActiveAccess(
            UUID userId,
            UUID programId
    ) {
        Instant now =
                clock.instant();

        return programAccessRepository
                .findActiveAccesses(
                        userId,
                        programId,
                        now
                )
                .stream()
                .findFirst();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveAccess(
            UUID userId,
            UUID programId
    ) {
        return findActiveAccess(
                userId,
                programId
        ).isPresent();
    }

    @Transactional(readOnly = true)
    public ProgramAccess requireActiveAccess(
            UUID userId,
            UUID programId
    ) {
        return findActiveAccess(
                userId,
                programId
        )
                .orElseThrow(
                        () -> new ProgramAccessDeniedException(
                                programId
                        )
                );
    }
}
