package com.fitnessplatform.program;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.fitnessplatform.media.*;
import java.util.UUID;

@Service
public class ProgramResourceService {

    private final ProgramResourceRepository programResourceRepository;
    private final ProgramRepository programRepository;

    private final MediaService mediaService;

    public ProgramResourceService(
            ProgramResourceRepository programResourceRepository,
            ProgramRepository programRepository,
            MediaService mediaService
    ) {
        this.programResourceRepository =
                programResourceRepository;

        this.programRepository =
                programRepository;
        this.mediaService = mediaService;
    }

    @Transactional
    public ProgramResource create(
            UUID programId,
            ProgramResourceRequest request
    ) {
        Program program =
                findProgram(programId);

        ensurePositionAvailable(
                programId,
                request.position()
        );

        MediaService.validateSources(request.url(), request.mediaId(), true);
        MediaUpload media = mediaService.attach(request.mediaId(), MediaPurpose.PROGRAM_RESOURCE, null);
        ProgramResource resource =
                new ProgramResource(
                        program,
                        request.title().strip(),
                        normalizeNullable(
                                request.description()
                        ),
                        normalizeNullable(request.url()),
                        request.position()
                );

        resource.setMedia(media);
        return programResourceRepository.save(
                resource
        );
    }

    @Transactional(readOnly = true)
    public List<ProgramResource> findAll(
            UUID programId
    ) {
        findProgram(programId);

        return programResourceRepository
                .findAllByProgramIdOrderByPositionAsc(
                        programId
                );
    }

    @Transactional(readOnly = true)
    public ProgramResource findById(
            UUID programId,
            UUID programResourceId
    ) {
        findProgram(programId);

        return findResource(
                programId,
                programResourceId
        );
    }

    @Transactional
    public ProgramResource update(
            UUID programId,
            UUID programResourceId,
            ProgramResourceRequest request
    ) {
        findProgram(programId);

        ProgramResource resource =
                findLockedResource(
                        programId,
                        programResourceId
                );

        ensurePositionAvailableForUpdate(
                programId,
                request.position(),
                programResourceId
        );

        MediaService.validateSources(request.url(), request.mediaId(), true);
        MediaUpload old = resource.getMedia();
        MediaUpload replacement = mediaService.attach(request.mediaId(), MediaPurpose.PROGRAM_RESOURCE, old);
        resource.update(
                request.title().strip(),
                normalizeNullable(
                        request.description()
                ),
                normalizeNullable(request.url()),
                request.position()
        );

        resource.setMedia(replacement);
        mediaService.retireReplaced(old, replacement);
        return resource;
    }

    @Transactional
    public void delete(
            UUID programId,
            UUID programResourceId
    ) {
        findProgram(programId);

        ProgramResource resource =
                findLockedResource(
                        programId,
                        programResourceId
                );

        programResourceRepository.delete(
                resource
        );
        mediaService.retireReplaced(resource.getMedia(), null);
    }

    private ProgramResource findLockedResource(UUID programId, UUID resourceId) {
        return programResourceRepository.findLockedByIdAndProgramId(resourceId, programId)
                .orElseThrow(() -> new ProgramResourceNotFoundException(resourceId));
    }

    private Program findProgram(
            UUID programId
    ) {
        return programRepository
                .findById(programId)
                .orElseThrow(
                        () ->
                                new ProgramNotFoundException(
                                        programId
                                )
                );
    }

    private ProgramResource findResource(
            UUID programId,
            UUID programResourceId
    ) {
        return programResourceRepository
                .findByIdAndProgramId(
                        programResourceId,
                        programId
                )
                .orElseThrow(
                        () ->
                                new ProgramResourceNotFoundException(
                                        programResourceId
                                )
                );
    }

    private void ensurePositionAvailable(
            UUID programId,
            Integer position
    ) {
        boolean positionAlreadyUsed =
                programResourceRepository
                        .existsByProgramIdAndPosition(
                                programId,
                                position
                        );

        if (positionAlreadyUsed) {
            throw new ProgramResourcePositionConflictException(
                    programId,
                    position
            );
        }
    }

    private void ensurePositionAvailableForUpdate(
            UUID programId,
            Integer position,
            UUID programResourceId
    ) {
        boolean positionAlreadyUsed =
                programResourceRepository
                        .existsByProgramIdAndPositionAndIdNot(
                                programId,
                                position,
                                programResourceId
                        );

        if (positionAlreadyUsed) {
            throw new ProgramResourcePositionConflictException(
                    programId,
                    position
            );
        }
    }

    private String normalizeNullable(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.strip();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}