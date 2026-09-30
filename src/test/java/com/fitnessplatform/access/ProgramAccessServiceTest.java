package com.fitnessplatform.access;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramNotFoundException;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserNotFoundException;
import com.fitnessplatform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramAccessServiceTest {

    @Mock
    private ProgramAccessRepository programAccessRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProgramRepository programRepository;

    private ProgramAccessService programAccessService;

    @BeforeEach
    void setUp() {
        programAccessService =
                new ProgramAccessService(
                        programAccessRepository,
                        userRepository,
                        programRepository
                );
    }

    @Test
    void shouldGrantProgramAccess() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        Instant startsAt =
                Instant.parse(
                        "2026-10-01T00:00:00Z"
                );

        Instant expiresAt =
                Instant.parse(
                        "2027-04-01T00:00:00Z"
                );

        User user =
                org.mockito.Mockito.mock(
                        User.class
                );

        Program program =
                org.mockito.Mockito.mock(
                        Program.class
                );

        when(
                user.getId()
        ).thenReturn(
                userId
        );

        when(
                program.getId()
        ).thenReturn(
                programId
        );

        when(
                userRepository.findById(
                        userId
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programAccessRepository.save(
                        any(ProgramAccess.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProgramAccessRequest request =
                new ProgramAccessRequest(
                        userId,
                        programId,
                        startsAt,
                        expiresAt
                );

        ProgramAccessResponse response =
                programAccessService.grant(
                        request
                );

        assertEquals(
                userId,
                response.userId()
        );

        assertEquals(
                programId,
                response.programId()
        );

        assertEquals(
                startsAt,
                response.startsAt()
        );

        assertEquals(
                expiresAt,
                response.expiresAt()
        );

        assertNull(
                response.revokedAt()
        );

        ArgumentCaptor<ProgramAccess> captor =
                ArgumentCaptor.forClass(
                        ProgramAccess.class
                );

        verify(
                programAccessRepository
        ).save(
                captor.capture()
        );

        ProgramAccess savedAccess =
                captor.getValue();

        assertEquals(
                userId,
                savedAccess.getUser().getId()
        );

        assertEquals(
                programId,
                savedAccess.getProgram().getId()
        );

        assertEquals(
                startsAt,
                savedAccess.getStartsAt()
        );

        assertEquals(
                expiresAt,
                savedAccess.getExpiresAt()
        );
    }

    @Test
    void shouldGrantProgramAccessWithoutExpiration() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        Instant startsAt =
                Instant.parse(
                        "2026-10-01T00:00:00Z"
                );

        User user =
                org.mockito.Mockito.mock(
                        User.class
                );

        Program program =
                org.mockito.Mockito.mock(
                        Program.class
                );

        when(
                user.getId()
        ).thenReturn(
                userId
        );

        when(
                program.getId()
        ).thenReturn(
                programId
        );

        when(
                userRepository.findById(
                        userId
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.of(program)
        );

        when(
                programAccessRepository.save(
                        any(ProgramAccess.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        ProgramAccessRequest request =
                new ProgramAccessRequest(
                        userId,
                        programId,
                        startsAt,
                        null
                );

        ProgramAccessResponse response =
                programAccessService.grant(
                        request
                );

        assertEquals(
                startsAt,
                response.startsAt()
        );

        assertNull(
                response.expiresAt()
        );
    }

    @Test
    void shouldThrowWhenGrantingAccessToUnknownUser() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        when(
                userRepository.findById(
                        userId
                )
        ).thenReturn(
                Optional.empty()
        );

        ProgramAccessRequest request =
                new ProgramAccessRequest(
                        userId,
                        programId,
                        Instant.parse(
                                "2026-10-01T00:00:00Z"
                        ),
                        null
                );

        assertThrows(
                UserNotFoundException.class,
                () ->
                        programAccessService.grant(
                                request
                        )
        );

        verify(
                programRepository,
                never()
        ).findById(
                any()
        );

        verify(
                programAccessRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void shouldThrowWhenGrantingAccessToUnknownProgram() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        User user =
                org.mockito.Mockito.mock(
                        User.class
                );

        when(
                userRepository.findById(
                        userId
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                programRepository.findById(
                        programId
                )
        ).thenReturn(
                Optional.empty()
        );

        ProgramAccessRequest request =
                new ProgramAccessRequest(
                        userId,
                        programId,
                        Instant.parse(
                                "2026-10-01T00:00:00Z"
                        ),
                        null
                );

        assertThrows(
                ProgramNotFoundException.class,
                () ->
                        programAccessService.grant(
                                request
                        )
        );

        verify(
                programAccessRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void shouldRejectExpirationEqualToStart() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        Instant startsAt =
                Instant.parse(
                        "2026-10-01T00:00:00Z"
                );

        ProgramAccessRequest request =
                new ProgramAccessRequest(
                        userId,
                        programId,
                        startsAt,
                        startsAt
                );

        assertThrows(
                ProgramAccessInvalidPeriodException.class,
                () ->
                        programAccessService.grant(
                                request
                        )
        );

        verify(
                userRepository,
                never()
        ).findById(
                any()
        );

        verify(
                programRepository,
                never()
        ).findById(
                any()
        );

        verify(
                programAccessRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void shouldRejectExpirationBeforeStart() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        Instant startsAt =
                Instant.parse(
                        "2026-10-01T00:00:00Z"
                );

        Instant expiresAt =
                Instant.parse(
                        "2026-09-30T00:00:00Z"
                );

        ProgramAccessRequest request =
                new ProgramAccessRequest(
                        userId,
                        programId,
                        startsAt,
                        expiresAt
                );

        assertThrows(
                ProgramAccessInvalidPeriodException.class,
                () ->
                        programAccessService.grant(
                                request
                        )
        );

        verify(
                programAccessRepository,
                never()
        ).save(
                any()
        );
    }

    @Test
    void shouldFindProgramAccessById() {
        UUID accessId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        Instant startsAt =
                Instant.parse(
                        "2026-10-01T00:00:00Z"
                );

        User user =
                org.mockito.Mockito.mock(
                        User.class
                );

        Program program =
                org.mockito.Mockito.mock(
                        Program.class
                );

        ProgramAccess access =
                org.mockito.Mockito.mock(
                        ProgramAccess.class
                );

        when(
                user.getId()
        ).thenReturn(
                userId
        );

        when(
                program.getId()
        ).thenReturn(
                programId
        );

        when(
                access.getId()
        ).thenReturn(
                accessId
        );

        when(
                access.getUser()
        ).thenReturn(
                user
        );

        when(
                access.getProgram()
        ).thenReturn(
                program
        );

        when(
                access.getStartsAt()
        ).thenReturn(
                startsAt
        );

        when(
                programAccessRepository.findById(
                        accessId
                )
        ).thenReturn(
                Optional.of(access)
        );

        ProgramAccessResponse response =
                programAccessService.findById(
                        accessId
                );

        assertEquals(
                accessId,
                response.id()
        );

        assertEquals(
                userId,
                response.userId()
        );

        assertEquals(
                programId,
                response.programId()
        );

        assertEquals(
                startsAt,
                response.startsAt()
        );
    }

    @Test
    void shouldThrowWhenProgramAccessDoesNotExist() {
        UUID accessId =
                UUID.randomUUID();

        when(
                programAccessRepository.findById(
                        accessId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramAccessNotFoundException.class,
                () ->
                        programAccessService.findById(
                                accessId
                        )
        );
    }

    @Test
    void shouldFilterProgramAccessesByUserAndProgram() {
        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        when(
                programAccessRepository
                        .findAllByUserIdAndProgramIdOrderByCreatedAtDesc(
                                userId,
                                programId
                        )
        ).thenReturn(
                List.of()
        );

        List<ProgramAccessResponse> result =
                programAccessService.findAll(
                        userId,
                        programId
                );

        assertTrue(
                result.isEmpty()
        );

        verify(
                programAccessRepository
        ).findAllByUserIdAndProgramIdOrderByCreatedAtDesc(
                userId,
                programId
        );
    }

    @Test
    void shouldFilterProgramAccessesByUser() {
        UUID userId =
                UUID.randomUUID();

        when(
                programAccessRepository
                        .findAllByUserIdOrderByCreatedAtDesc(
                                userId
                        )
        ).thenReturn(
                List.of()
        );

        List<ProgramAccessResponse> result =
                programAccessService.findAll(
                        userId,
                        null
                );

        assertTrue(
                result.isEmpty()
        );

        verify(
                programAccessRepository
        ).findAllByUserIdOrderByCreatedAtDesc(
                userId
        );
    }

    @Test
    void shouldFilterProgramAccessesByProgram() {
        UUID programId =
                UUID.randomUUID();

        when(
                programAccessRepository
                        .findAllByProgramIdOrderByCreatedAtDesc(
                                programId
                        )
        ).thenReturn(
                List.of()
        );

        List<ProgramAccessResponse> result =
                programAccessService.findAll(
                        null,
                        programId
                );

        assertTrue(
                result.isEmpty()
        );

        verify(
                programAccessRepository
        ).findAllByProgramIdOrderByCreatedAtDesc(
                programId
        );
    }

    @Test
    void shouldListAllProgramAccesses() {
        when(
                programAccessRepository
                        .findAllByOrderByCreatedAtDesc()
        ).thenReturn(
                List.of()
        );

        List<ProgramAccessResponse> result =
                programAccessService.findAll(
                        null,
                        null
                );

        assertTrue(
                result.isEmpty()
        );

        verify(
                programAccessRepository
        ).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void shouldRevokeProgramAccess() {
        UUID accessId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        UUID programId =
                UUID.randomUUID();

        User user =
                org.mockito.Mockito.mock(
                        User.class
                );

        Program program =
                org.mockito.Mockito.mock(
                        Program.class
                );

        when(
                user.getId()
        ).thenReturn(
                userId
        );

        when(
                program.getId()
        ).thenReturn(
                programId
        );

        ProgramAccess access =
                new ProgramAccess(
                        user,
                        program,
                        Instant.parse(
                                "2026-10-01T00:00:00Z"
                        ),
                        null
                );

        when(
                programAccessRepository.findById(
                        accessId
                )
        ).thenReturn(
                Optional.of(access)
        );

        ProgramAccessResponse response =
                programAccessService.revoke(
                        accessId
                );

        assertNotNull(
                response.revokedAt()
        );

        assertNotNull(
                access.getRevokedAt()
        );
    }

    @Test
    void shouldPreserveOriginalRevocationTimeWhenRevokedAgain() {
        UUID accessId =
                UUID.randomUUID();

        User user =
                org.mockito.Mockito.mock(
                        User.class
                );

        Program program =
                org.mockito.Mockito.mock(
                        Program.class
                );

        when(
                user.getId()
        ).thenReturn(
                UUID.randomUUID()
        );

        when(
                program.getId()
        ).thenReturn(
                UUID.randomUUID()
        );

        ProgramAccess access =
                new ProgramAccess(
                        user,
                        program,
                        Instant.parse(
                                "2026-10-01T00:00:00Z"
                        ),
                        null
                );

        Instant originalRevocation =
                Instant.parse(
                        "2026-10-10T12:00:00Z"
                );

        access.revoke(
                originalRevocation
        );

        when(
                programAccessRepository.findById(
                        accessId
                )
        ).thenReturn(
                Optional.of(access)
        );

        ProgramAccessResponse response =
                programAccessService.revoke(
                        accessId
                );

        assertEquals(
                originalRevocation,
                access.getRevokedAt()
        );

        assertEquals(
                originalRevocation,
                response.revokedAt()
        );
    }

    @Test
    void shouldThrowWhenRevokingUnknownProgramAccess() {
        UUID accessId =
                UUID.randomUUID();

        when(
                programAccessRepository.findById(
                        accessId
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThrows(
                ProgramAccessNotFoundException.class,
                () ->
                        programAccessService.revoke(
                                accessId
                        )
        );
    }
}