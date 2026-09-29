package com.fitnessplatform.program;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;

    public ProgramService(
            ProgramRepository programRepository
    ) {
        this.programRepository = programRepository;
    }

    @Transactional
    public Program create(
            ProgramRequest request
    ) {
        Program program = new Program(
                request.name().strip(),
                normalizeNullable(request.description())
        );

        return programRepository.save(program);
    }

    @Transactional(readOnly = true)
    public List<Program> findAll() {
        return programRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Program findById(UUID programId) {
        return programRepository
                .findById(programId)
                .orElseThrow(
                        () -> new ProgramNotFoundException(programId)
                );
    }

    @Transactional
    public Program update(
            UUID programId,
            ProgramRequest request
    ) {
        Program program = findById(programId);

        program.update(
                request.name().strip(),
                normalizeNullable(request.description())
        );

        return program;
    }

    @Transactional
    public Program publish(UUID programId) {
        Program program = findById(programId);

        program.publish();

        return program;
    }

    @Transactional
    public Program moveToDraft(UUID programId) {
        Program program = findById(programId);

        program.moveToDraft();

        return program;
    }

    @Transactional
    public void delete(UUID programId) {
        Program program = findById(programId);

        programRepository.delete(program);
    }


    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        return normalized.isEmpty()
                ? null
                :normalized;
    }
}
