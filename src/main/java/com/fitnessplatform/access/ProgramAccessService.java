package com.fitnessplatform.access;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramNotFoundException;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserNotFoundException;
import com.fitnessplatform.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProgramAccessService {

    private final ProgramAccessRepository
            programAccessRepository;

    private final UserRepository
            userRepository;

    private final ProgramRepository
            programRepository;

    public ProgramAccessService(
            ProgramAccessRepository programAccessRepository,
            UserRepository userRepository,
            ProgramRepository programRepository
    ) {
        this.programAccessRepository =
                programAccessRepository;

        this.userRepository =
                userRepository;

        this.programRepository =
                programRepository;
    }

    @Transactional
    public ProgramAccessResponse grant(
            ProgramAccessRequest request
    ) {
        validatePeriod(
                request.startsAt(),
                request.expiresAt()
        );

        User user =
                userRepository
                        .findById(
                                request.userId()
                        )
                        .orElseThrow(
                                () ->
                                        new UserNotFoundException(
                                                request.userId()
                                        )
                        );

        Program program =
                programRepository
                        .findById(
                                request.programId()
                        )
                        .orElseThrow(
                                () ->
                                        new ProgramNotFoundException(
                                                request.programId()
                                        )
                        );

        ProgramAccess access =
                new ProgramAccess(
                        user,
                        program,
                        request.startsAt(),
                        request.expiresAt()
                );

        ProgramAccess saved =
                programAccessRepository.save(
                        access
                );

        return ProgramAccessResponse.from(
                saved
        );
    }

    @Transactional(readOnly = true)
    public ProgramAccessResponse findById(
            UUID accessId
    ) {
        return ProgramAccessResponse.from(
                getAccess(
                        accessId
                )
        );
    }

    @Transactional(readOnly = true)
    public List<ProgramAccessResponse> findAll(
            UUID userId,
            UUID programId
    ) {
        List<ProgramAccess> accesses;

        if (
                userId != null
                        && programId != null
        ) {
            accesses =
                    programAccessRepository
                            .findAllByUserIdAndProgramIdOrderByCreatedAtDesc(
                                    userId,
                                    programId
                            );
        } else if (userId != null) {
            accesses =
                    programAccessRepository
                            .findAllByUserIdOrderByCreatedAtDesc(
                                    userId
                            );
        } else if (programId != null) {
            accesses =
                    programAccessRepository
                            .findAllByProgramIdOrderByCreatedAtDesc(
                                    programId
                            );
        } else {
            accesses =
                    programAccessRepository
                            .findAllByOrderByCreatedAtDesc();
        }

        return accesses.stream()
                .map(
                        ProgramAccessResponse::from
                )
                .toList();
    }

    @Transactional
    public ProgramAccessResponse revoke(
            UUID accessId
    ) {
        ProgramAccess access =
                getAccess(
                        accessId
                );

        if (access.getRevokedAt() == null) {
            access.revoke(
                    Instant.now()
            );
        }

        return ProgramAccessResponse.from(
                access
        );
    }

    private ProgramAccess getAccess(
            UUID accessId
    ) {
        return programAccessRepository
                .findById(
                        accessId
                )
                .orElseThrow(
                        () ->
                                new ProgramAccessNotFoundException(
                                        accessId
                                )
                );
    }

    private void validatePeriod(
            Instant startsAt,
            Instant expiresAt
    ) {
        if (
                expiresAt != null
                        && !expiresAt.isAfter(
                        startsAt
                )
        ) {
            throw new ProgramAccessInvalidPeriodException();
        }
    }
}