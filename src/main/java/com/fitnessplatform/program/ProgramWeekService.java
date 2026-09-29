package com.fitnessplatform.program;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProgramWeekService {

    private final ProgramWeekRepository programWeekRepository;
    private final ProgramRepository programRepository;

    public ProgramWeekService(
            ProgramWeekRepository programWeekRepository,
            ProgramRepository programRepository
    ) {
        this.programWeekRepository = programWeekRepository;
        this.programRepository = programRepository;
    }

    @Transactional
    public ProgramWeek create(
            UUID programId,
            ProgramWeekRequest request
    ) {
        Program program =
                findProgram(programId);

        ensurePositionAvailable(
                programId,
                request.position()
        );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        request.title().strip(),
                        normalizeNullable(request.description()),
                        request.position()
                );

        return programWeekRepository.save(programWeek);
    }

    @Transactional(readOnly = true)
    public List<ProgramWeek> findAll(
            UUID programId
    ) {
        findProgram(programId);

        return programWeekRepository
                .findAllByProgramIdOrderByPositionAsc(
                        programId
                );
    }

    @Transactional(readOnly = true)
    public ProgramWeek findById(
            UUID programId,
            UUID programWeekId
    ) {
        findProgram(programId);

        return findProgramWeek(
                programId,
                programWeekId
        );
    }

    @Transactional
    public ProgramWeek update(
            UUID programId,
            UUID programWeekId,
            ProgramWeekRequest request
    ) {
        findProgram(programId);

        ProgramWeek programWeek =
                findProgramWeek(
                        programId,
                        programWeekId
                );

        ensurePositionAvailableForUpdate(
                programId,
                request.position(),
                programWeekId
        );

        programWeek.update(
                request.title().strip(),
                normalizeNullable(request.description()),
                request.position()
        );

        return programWeek;
    }

    @Transactional
    public void delete(
            UUID programId,
            UUID programWeekId
    ) {
        findProgram(programId);

        ProgramWeek programWeek =
                findProgramWeek(
                programId,
                programWeekId
        );

        programWeekRepository.delete(programWeek);
    }

    private Program findProgram(
            UUID programId
    ) {
        return programRepository
                .findById(programId)
                .orElseThrow(
                        () -> new ProgramNotFoundException(
                                programId
                        )
                );
    }

    private ProgramWeek findProgramWeek(
            UUID programId,
            UUID programWeekId
    ) {
        return programWeekRepository
                .findByIdAndProgramId(
                        programWeekId,
                        programId
                )
                .orElseThrow(
                        () -> new ProgramWeekNotFoundException(
                                programWeekId
                        )
                );
    }

    private void ensurePositionAvailable(
            UUID programId,
            Integer position
    ) {
        boolean positionAlreadyUsed =
                programWeekRepository
                        .existsByProgramIdAndPosition(
                                programId,
                                position
                        );

        if (positionAlreadyUsed) {
            throw new ProgramWeekPositionConflictException(
                    programId,
                    position
            );
        }
    }

    private void ensurePositionAvailableForUpdate(
            UUID programId,
            Integer position,
            UUID programWeekId
    ) {
        boolean positionAlreadyUsed =
                programWeekRepository
                        .existsByProgramIdAndPositionAndIdNot(
                                programId,
                                position,
                                programWeekId
                        );

        if (positionAlreadyUsed) {
            throw new ProgramWeekPositionConflictException(
                    programId,
                    position
            );
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
