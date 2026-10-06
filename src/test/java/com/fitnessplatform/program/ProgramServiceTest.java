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
class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    private ProgramService programService;

    @BeforeEach
    void setUp() {
        programService =
                new ProgramService(
                        programRepository
                );
    }

    @Test
    void shouldCreateProgramAsDraft() {
        ProgramRequest request =
                new ProgramRequest(
                        "  Six Week Strength  ",
                        "  Strength program.  "
                );

        when(
                programRepository.save(
                        any(Program.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        Program program =
                programService.create(request);

        assertEquals(
                "Six Week Strength",
                program.getName()
        );

        assertEquals(
                "Strength program.",
                program.getDescription()
        );

        assertEquals(
                ProgramStatus.DRAFT,
                program.getStatus()
        );

        verify(programRepository)
                .save(program);
    }

    @Test
    void shouldConvertBlankDescriptionToNull() {
        ProgramRequest request =
                new ProgramRequest(
                        "Strength Program",
                        "   "
                );

        when(
                programRepository.save(
                        any(Program.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        Program program =
                programService.create(request);

        assertNull(
                program.getDescription()
        );
    }

    @Test
    void shouldReturnProgramById() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.of(program)
        );

        Program result =
                programService.findById(
                        programId
                );

        assertSame(
                program,
                result
        );
    }

    @Test
    void shouldThrowWhenProgramDoesNotExist() {
        UUID programId =
                UUID.randomUUID();

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramNotFoundException.class,
                () ->
                        programService.findById(
                                programId
                        )
        );
    }

    @Test
    void shouldReturnAllPrograms() {
        Program first =
                new Program(
                        "Program A",
                        null
                );

        Program second =
                new Program(
                        "Program B",
                        null
                );

        when(programRepository.findAll())
                .thenReturn(
                        List.of(first, second)
                );

        List<Program> result =
                programService.findAll();

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
    void shouldUpdateProgramWithoutChangingStatus() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Old Name",
                        "Old description"
                );

        program.publish();

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.of(program)
        );

        ProgramRequest request =
                new ProgramRequest(
                        "Updated Program",
                        "Updated description"
                );

        Program result =
                programService.update(
                        programId,
                        request
                );

        assertSame(
                program,
                result
        );

        assertEquals(
                "Updated Program",
                result.getName()
        );

        assertEquals(
                "Updated description",
                result.getDescription()
        );

        assertEquals(
                ProgramStatus.PUBLISHED,
                result.getStatus()
        );
    }

    @Test
    void shouldPublishProgram() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.of(program)
        );

        Program result =
                programService.publish(
                        programId
                );

        assertEquals(
                ProgramStatus.PUBLISHED,
                result.getStatus()
        );
    }

    @Test
    void shouldMoveProgramBackToDraft() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        program.publish();

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.of(program)
        );

        Program result =
                programService.moveToDraft(
                        programId
                );

        assertEquals(
                ProgramStatus.DRAFT,
                result.getStatus()
        );
    }

    @Test
    void shouldDeleteExistingProgram() {
        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.of(program)
        );

        programService.delete(
                programId
        );

        verify(programRepository)
                .delete(program);
    }

    @Test
    void shouldUpdateProgramPricing() {

        UUID programId =
                UUID.randomUUID();

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.of(program)
        );

        ProgramPricingRequest request =
                new ProgramPricingRequest(
                        4999L
                );

        Program result =
                programService.updatePricing(
                        programId,
                        request
                );

        assertSame(
                program,
                result
        );

        assertEquals(
                4999L,
                result.getPriceCents()
        );

        assertEquals(
                "USD",
                result.getCurrency()
        );
    }
}