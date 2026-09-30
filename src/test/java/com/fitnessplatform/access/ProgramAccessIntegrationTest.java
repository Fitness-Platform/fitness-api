package com.fitnessplatform.access;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProgramAccessIntegrationTest {

    @Autowired
    private ProgramAccessRepository programAccessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        programAccessRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        programRepository.deleteAll();
    }

    @Test
    void shouldPersistProgramAccess() {
        User user =
                createUser("member@example.com");

        Program program =
                createProgram("Strength Program");

        Instant startsAt =
                Instant.now()
                        .truncatedTo(ChronoUnit.MILLIS);

        Instant expiresAt =
                startsAt.plus(
                        180,
                        ChronoUnit.DAYS
                );

        ProgramAccess access =
                programAccessRepository.saveAndFlush(
                        new ProgramAccess(
                                user,
                                program,
                                startsAt,
                                expiresAt
                        )
                );

        ProgramAccess persisted =
                programAccessRepository
                        .findById(access.getId())
                        .orElseThrow();

        assertEquals(
                user.getId(),
                persisted.getUser().getId()
        );

        assertEquals(
                program.getId(),
                persisted.getProgram().getId()
        );

        assertEquals(
                startsAt,
                persisted.getStartsAt()
        );

        assertEquals(
                expiresAt,
                persisted.getExpiresAt()
        );

        assertNull(
                persisted.getRevokedAt()
        );

        assertNotNull(
                persisted.getCreatedAt()
        );

        assertNotNull(
                persisted.getUpdatedAt()
        );
    }

    @Test
    void shouldAllowAccessWithoutExpiration() {
        User user =
                createUser("member@example.com");

        Program program =
                createProgram("Private Coaching");

        ProgramAccess access =
                programAccessRepository.saveAndFlush(
                        new ProgramAccess(
                                user,
                                program,
                                Instant.now(),
                                null
                        )
                );

        ProgramAccess persisted =
                programAccessRepository
                        .findById(access.getId())
                        .orElseThrow();

        assertNull(
                persisted.getExpiresAt()
        );

        assertNull(
                persisted.getRevokedAt()
        );
    }

    @Test
    void shouldPersistRevocationWithoutDeletingAccess() {
        User user =
                createUser("member@example.com");

        Program program =
                createProgram("Strength Program");

        ProgramAccess access =
                programAccessRepository.saveAndFlush(
                        new ProgramAccess(
                                user,
                                program,
                                Instant.now(),
                                null
                        )
                );

        Instant revokedAt =
                Instant.now()
                        .plusSeconds(1)
                        .truncatedTo(ChronoUnit.MILLIS);

        access.revoke(
                revokedAt
        );

        programAccessRepository.saveAndFlush(
                access
        );

        ProgramAccess persisted =
                programAccessRepository
                        .findById(access.getId())
                        .orElseThrow();

        assertEquals(
                revokedAt,
                persisted.getRevokedAt()
        );

        assertTrue(
                programAccessRepository.existsById(
                        access.getId()
                )
        );
    }

    @Test
    void shouldAllowMultipleAccessesForSameUserAndProgram() {
        User user =
                createUser("member@example.com");

        Program program =
                createProgram("Strength Program");

        Instant firstStart =
                Instant.parse(
                        "2026-01-01T00:00:00Z"
                );

        Instant secondStart =
                Instant.parse(
                        "2027-01-01T00:00:00Z"
                );

        ProgramAccess firstAccess =
                programAccessRepository.saveAndFlush(
                        new ProgramAccess(
                                user,
                                program,
                                firstStart,
                                firstStart.plus(
                                        180,
                                        ChronoUnit.DAYS
                                )
                        )
                );

        ProgramAccess secondAccess =
                programAccessRepository.saveAndFlush(
                        new ProgramAccess(
                                user,
                                program,
                                secondStart,
                                secondStart.plus(
                                        180,
                                        ChronoUnit.DAYS
                                )
                        )
                );

        List<ProgramAccess> accesses =
                programAccessRepository
                        .findAllByUserIdAndProgramIdOrderByCreatedAtDesc(
                                user.getId(),
                                program.getId()
                        );

        assertEquals(
                2,
                accesses.size()
        );

        assertTrue(
                accesses.stream()
                        .anyMatch(access ->
                                access.getId()
                                        .equals(firstAccess.getId())
                        )
        );

        assertTrue(
                accesses.stream()
                        .anyMatch(access ->
                                access.getId()
                                        .equals(secondAccess.getId())
                        )
        );
    }

    @Test
    void shouldRejectExpirationThatIsNotAfterStart() {
        User user =
                createUser("member@example.com");

        Program program =
                createProgram("Strength Program");

        Instant startsAt =
                Instant.now();

        ProgramAccess invalidAccess =
                new ProgramAccess(
                        user,
                        program,
                        startsAt,
                        startsAt
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        programAccessRepository
                                .saveAndFlush(
                                        invalidAccess
                                )
        );
    }

    @Test
    void shouldPreventDeletingUserReferencedByProgramAccess() {
        User user =
                createUser("member@example.com");

        Program program =
                createProgram("Strength Program");

        programAccessRepository.saveAndFlush(
                new ProgramAccess(
                        user,
                        program,
                        Instant.now(),
                        null
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    userRepository.delete(user);
                    userRepository.flush();
                }
        );

        assertTrue(
                userRepository.existsById(
                        user.getId()
                )
        );
    }

    @Test
    void shouldPreventDeletingProgramReferencedByProgramAccess() {
        User user =
                createUser("member@example.com");

        Program program =
                createProgram("Strength Program");

        programAccessRepository.saveAndFlush(
                new ProgramAccess(
                        user,
                        program,
                        Instant.now(),
                        null
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    programRepository.delete(program);
                    programRepository.flush();
                }
        );

        assertTrue(
                programRepository.existsById(
                        program.getId()
                )
        );
    }

    private User createUser(
            String email
    ) {
        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(
                                "StrongPassword123!"
                        ),
                        UserRole.USER
                )
        );
    }

    private Program createProgram(
            String name
    ) {
        return programRepository.saveAndFlush(
                new Program(
                        name,
                        null
                )
        );
    }
}