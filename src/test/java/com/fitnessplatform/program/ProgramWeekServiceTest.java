package com.fitnessplatform.program;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramWeekServiceTest {

    @Mock
    private ProgramWeekRepository programWeekRepository;

    @Mock
    private ProgramRepository programRepository;

    private ProgramWeekService programWeekService;

    @BeforeEach
    void setUp() {
        programWeekService =
                new ProgramWeekService(
                        programWeekRepository,
                        programRepository
                );
    }

    @Test
    void shouldCreateProgramWeek() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeekRequest request =
                new ProgramWeekRequest(
                        "  Week 1  ",
                        "  Foundation week.  ",
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository
                        .existsByProgramIdAndPosition(
                                programId,
                                1
                        )
        ).thenReturn(false);

        when(
                programWeekRepository.save(
                        any(ProgramWeek.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProgramWeek result =
                programWeekService.create(
                        programId,
                        request
                );

        assertSame(
                program,
                result.getProgram()
        );

        assertEquals(
                "Week 1",
                result.getTitle()
        );

        assertEquals(
                "Foundation week.",
                result.getDescription()
        );

        assertEquals(
                1,
                result.getPosition()
        );

        verify(programWeekRepository)
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
                programWeekRepository
                        .existsByProgramIdAndPosition(
                                programId,
                                1
                        )
        ).thenReturn(false);

        when(
                programWeekRepository.save(
                        any(ProgramWeek.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProgramWeek result =
                programWeekService.create(
                        programId,
                        new ProgramWeekRequest(
                                "Week 1",
                                "   ",
                                1
                        )
                );

        assertNull(
                result.getDescription()
        );
    }

    @Test
    void shouldThrowWhenCreatingWeekForMissingProgram() {
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
                        programWeekService.create(
                                programId,
                                new ProgramWeekRequest(
                                        "Week 1",
                                        null,
                                        1
                                )
                        )
        );

        verifyNoInteractions(
                programWeekRepository
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
                programWeekRepository
                        .existsByProgramIdAndPosition(
                                programId,
                                1
                        )
        ).thenReturn(true);

        assertThrows(
                ProgramWeekPositionConflictException.class,
                () ->
                        programWeekService.create(
                                programId,
                                new ProgramWeekRequest(
                                        "Week 1",
                                        null,
                                        1
                                )
                        )
        );

        verify(
                programWeekRepository,
                never()
        ).save(any());
    }

    @Test
    void shouldReturnWeeksOrderedByPosition() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek first =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        ProgramWeek second =
                new ProgramWeek(
                        program,
                        "Week 2",
                        null,
                        2
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                programId
                        )
        ).thenReturn(
                List.of(first, second)
        );

        List<ProgramWeek> result =
                programWeekService.findAll(
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
    void shouldThrowWhenListingWeeksForMissingProgram() {
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
                        programWeekService.findAll(
                                programId
                        )
        );

        verify(
                programWeekRepository,
                never()
        ).findAllByProgramIdOrderByPositionAsc(
                any()
        );
    }

    @Test
    void shouldReturnProgramWeekById() {
        UUID programId =
                UUID.randomUUID();

        UUID weekId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository
                        .findByIdAndProgramId(
                                weekId,
                                programId
                        )
        ).thenReturn(
                Optional.of(programWeek)
        );

        ProgramWeek result =
                programWeekService.findById(
                        programId,
                        weekId
                );

        assertSame(
                programWeek,
                result
        );
    }

    @Test
    void shouldRejectWeekFromDifferentProgram() {
        UUID requestedProgramId =
                UUID.randomUUID();

        UUID weekId =
                UUID.randomUUID();

        Program requestedProgram =
                new Program(
                        "Program B",
                        null
                );

        when(
                programRepository.findById(
                        requestedProgramId
                )
        ).thenReturn(
                Optional.of(requestedProgram)
        );

        when(
                programWeekRepository
                        .findByIdAndProgramId(
                                weekId,
                                requestedProgramId
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramWeekNotFoundException.class,
                () ->
                        programWeekService.findById(
                                requestedProgramId,
                                weekId
                        )
        );
    }

    @Test
    void shouldUpdateProgramWeek() {
        UUID programId =
                UUID.randomUUID();

        UUID weekId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Old title",
                        "Old description",
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository
                        .findByIdAndProgramId(
                                weekId,
                                programId
                        )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                programWeekRepository
                        .existsByProgramIdAndPositionAndIdNot(
                                programId,
                                2,
                                weekId
                        )
        ).thenReturn(false);

        ProgramWeek result =
                programWeekService.update(
                        programId,
                        weekId,
                        new ProgramWeekRequest(
                                "Updated Week",
                                "Updated description",
                                2
                        )
                );

        assertEquals(
                "Updated Week",
                result.getTitle()
        );

        assertEquals(
                "Updated description",
                result.getDescription()
        );

        assertEquals(
                2,
                result.getPosition()
        );
    }

    @Test
    void shouldRejectDuplicatePositionOnUpdate() {
        UUID programId =
                UUID.randomUUID();

        UUID weekId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 2",
                        null,
                        2
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository
                        .findByIdAndProgramId(
                                weekId,
                                programId
                        )
        ).thenReturn(
                Optional.of(programWeek)
        );

        when(
                programWeekRepository
                        .existsByProgramIdAndPositionAndIdNot(
                                programId,
                                1,
                                weekId
                        )
        ).thenReturn(true);

        assertThrows(
                ProgramWeekPositionConflictException.class,
                () ->
                        programWeekService.update(
                                programId,
                                weekId,
                                new ProgramWeekRequest(
                                        "Week 2",
                                        null,
                                        1
                                )
                        )
        );

        assertEquals(
                2,
                programWeek.getPosition()
        );
    }

    @Test
    void shouldDeleteProgramWeek() {
        UUID programId =
                UUID.randomUUID();

        UUID weekId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        ProgramWeek programWeek =
                new ProgramWeek(
                        program,
                        "Week 1",
                        null,
                        1
                );

        when(
                programRepository.findById(programId)
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programWeekRepository
                        .findByIdAndProgramId(
                                weekId,
                                programId
                        )
        ).thenReturn(
                Optional.of(programWeek)
        );

        programWeekService.delete(
                programId,
                weekId
        );

        verify(programWeekRepository)
                .delete(programWeek);
    }
}