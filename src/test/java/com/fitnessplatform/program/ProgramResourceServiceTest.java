package com.fitnessplatform.program;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramResourceServiceTest {

    @Mock
    private ProgramResourceRepository programResourceRepository;

    @Mock
    private ProgramRepository programRepository;

    private ProgramResourceService programResourceService;

    @BeforeEach
    void setUp() {
        programResourceService =
                new ProgramResourceService(
                        programResourceRepository,
                        programRepository
                );
    }

    @Test
    void shouldCreateProgramResource() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramResourceRequest request =
                new ProgramResourceRequest(
                        "  Nutrition Guide  ",
                        "  Supporting material.  ",
                        "  https://example.com/guide.pdf  ",
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programResourceRepository
                        .existsByProgramIdAndPosition(
                                programId,
                                1
                        )
        ).thenReturn(false);

        when(
                programResourceRepository.save(
                        any(ProgramResource.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProgramResource result =
                programResourceService.create(
                        programId,
                        request
                );

        assertSame(
                program,
                result.getProgram()
        );

        assertEquals(
                "Nutrition Guide",
                result.getTitle()
        );

        assertEquals(
                "Supporting material.",
                result.getDescription()
        );

        assertEquals(
                "https://example.com/guide.pdf",
                result.getUrl()
        );

        assertEquals(
                1,
                result.getPosition()
        );

        verify(programResourceRepository)
                .save(result);
    }

    @Test
    void shouldNormalizeBlankDescriptionToNull() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programResourceRepository
                        .existsByProgramIdAndPosition(
                                programId,
                                1
                        )
        ).thenReturn(false);

        when(
                programResourceRepository.save(
                        any(ProgramResource.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProgramResource result =
                programResourceService.create(
                        programId,
                        new ProgramResourceRequest(
                                "Nutrition Guide",
                                "   ",
                                "https://example.com/guide",
                                1
                        )
                );

        assertNull(
                result.getDescription()
        );
    }

    @Test
    void shouldRejectMissingProgramOnCreate() {
        UUID programId =
                UUID.randomUUID();

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramNotFoundException.class,
                () ->
                        programResourceService.create(
                                programId,
                                new ProgramResourceRequest(
                                        "Guide",
                                        null,
                                        "https://example.com",
                                        1
                                )
                        )
        );

        verifyNoInteractions(
                programResourceRepository
        );
    }

    @Test
    void shouldRejectDuplicatePositionOnCreate() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programResourceRepository
                        .existsByProgramIdAndPosition(
                                programId,
                                1
                        )
        ).thenReturn(true);

        assertThrows(
                ProgramResourcePositionConflictException.class,
                () ->
                        programResourceService.create(
                                programId,
                                new ProgramResourceRequest(
                                        "Guide",
                                        null,
                                        "https://example.com",
                                        1
                                )
                        )
        );

        verify(
                programResourceRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldReturnResourcesOrderedByPosition() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramResource first =
                new ProgramResource(
                        program,
                        "Guide",
                        null,
                        "https://example.com/guide",
                        1
                );

        ProgramResource second =
                new ProgramResource(
                        program,
                        "Checklist",
                        null,
                        "https://example.com/checklist",
                        2
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programResourceRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                programId
                        )
        ).thenReturn(
                List.of(first, second)
        );

        List<ProgramResource> result =
                programResourceService.findAll(
                        programId
                );

        assertEquals(
                2,
                result.size()
        );

        assertSame(
                first,
                result.get(0)
        );

        assertSame(
                second,
                result.get(1)
        );
    }

    @Test
    void shouldRejectResourceFromDifferentProgram() {
        UUID programId =
                UUID.randomUUID();

        UUID resourceId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Program B",
                        null
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programResourceRepository
                        .findByIdAndProgramId(
                                resourceId,
                                programId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramResourceNotFoundException.class,
                () ->
                        programResourceService.findById(
                                programId,
                                resourceId
                        )
        );
    }

    @Test
    void shouldUpdateProgramResource() {
        UUID programId =
                UUID.randomUUID();

        UUID resourceId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramResource resource =
                new ProgramResource(
                        program,
                        "Old Guide",
                        "Old description",
                        "https://example.com/old",
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programResourceRepository
                        .findByIdAndProgramId(
                                resourceId,
                                programId
                        )
        ).thenReturn(
                Optional.of(resource)
        );

        when(
                programResourceRepository
                        .existsByProgramIdAndPositionAndIdNot(
                                programId,
                                2,
                                resourceId
                        )
        ).thenReturn(false);

        ProgramResource result =
                programResourceService.update(
                        programId,
                        resourceId,
                        new ProgramResourceRequest(
                                "Updated Guide",
                                "Updated description",
                                "https://example.com/new",
                                2
                        )
                );

        assertEquals(
                "Updated Guide",
                result.getTitle()
        );

        assertEquals(
                "Updated description",
                result.getDescription()
        );

        assertEquals(
                "https://example.com/new",
                result.getUrl()
        );

        assertEquals(
                2,
                result.getPosition()
        );
    }

    @Test
    void shouldAllowResourceToKeepItsOwnPosition() {
        UUID programId =
                UUID.randomUUID();

        UUID resourceId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramResource resource =
                new ProgramResource(
                        program,
                        "Guide",
                        null,
                        "https://example.com",
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programResourceRepository
                        .findByIdAndProgramId(
                                resourceId,
                                programId
                        )
        ).thenReturn(
                Optional.of(resource)
        );

        when(
                programResourceRepository
                        .existsByProgramIdAndPositionAndIdNot(
                                programId,
                                1,
                                resourceId
                        )
        ).thenReturn(false);

        ProgramResource result =
                programResourceService.update(
                        programId,
                        resourceId,
                        new ProgramResourceRequest(
                                "Updated Guide",
                                null,
                                "https://example.com/updated",
                                1
                        )
                );

        assertEquals(
                1,
                result.getPosition()
        );
    }

    @Test
    void shouldRejectDuplicatePositionOnUpdate() {
        UUID programId =
                UUID.randomUUID();

        UUID resourceId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramResource resource =
                new ProgramResource(
                        program,
                        "Guide",
                        null,
                        "https://example.com",
                        2
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programResourceRepository
                        .findByIdAndProgramId(
                                resourceId,
                                programId
                        )
        ).thenReturn(
                Optional.of(resource)
        );

        when(
                programResourceRepository
                        .existsByProgramIdAndPositionAndIdNot(
                                programId,
                                1,
                                resourceId
                        )
        ).thenReturn(true);

        assertThrows(
                ProgramResourcePositionConflictException.class,
                () ->
                        programResourceService.update(
                                programId,
                                resourceId,
                                new ProgramResourceRequest(
                                        "Guide",
                                        null,
                                        "https://example.com",
                                        1
                                )
                        )
        );

        assertEquals(
                2,
                resource.getPosition()
        );
    }

    @Test
    void shouldDeleteProgramResource() {
        UUID programId =
                UUID.randomUUID();

        UUID resourceId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramResource resource =
                new ProgramResource(
                        program,
                        "Guide",
                        null,
                        "https://example.com",
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programResourceRepository
                        .findByIdAndProgramId(
                                resourceId,
                                programId
                        )
        ).thenReturn(
                Optional.of(resource)
        );

        programResourceService.delete(
                programId,
                resourceId
        );

        verify(programResourceRepository)
                .delete(resource);
    }
}