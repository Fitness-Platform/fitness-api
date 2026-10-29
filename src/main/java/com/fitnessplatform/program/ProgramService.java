package com.fitnessplatform.program;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.fitnessplatform.media.MediaService;
import java.util.UUID;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;

    private final ProgramResourceRepository resourceRepository;
    private final MediaService mediaService;

    public ProgramService(
            ProgramRepository programRepository,
            ProgramResourceRepository resourceRepository,
            MediaService mediaService
    ) {
        this.programRepository = programRepository;
        this.resourceRepository = resourceRepository;
        this.mediaService = mediaService;
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

    @Transactional(readOnly = true)
    public List<Program> findPublished() {
        return programRepository.findByStatusOrderByCreatedAtDescIdAsc(
                ProgramStatus.PUBLISHED
        );
    }

    @Transactional(readOnly = true)
    public Program findPublishedById(UUID programId) {
        return programRepository.findByIdAndStatus(programId, ProgramStatus.PUBLISHED)
                .orElseThrow(() -> new ProgramNotFoundException(programId));
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
    public Program updatePricing(
            UUID programId,
            ProgramPricingRequest request
    ) {
        Program program =
                findById(
                        programId
                );

        program.updatePrice(
                request.priceCents()
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
        // Serialize deletion with new child inserts before collecting owned media.
        Program program = programRepository.findLockedById(programId)
                .orElseThrow(() -> new ProgramNotFoundException(programId));

        var resources = resourceRepository.findAllLockedByProgramId(programId);
        for (ProgramResource resource : resources) {
            mediaService.retireReplaced(resource.getMedia(), null);
        }
        // Explicitly delete loaded children before their parent; database cascades alone
        // leave managed children referencing a deleted parent during Hibernate flush.
        resourceRepository.deleteAll(resources);
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
