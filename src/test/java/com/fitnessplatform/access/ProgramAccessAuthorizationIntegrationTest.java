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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import({
        TestcontainersConfiguration.class,
        ProgramAccessAuthorizationIntegrationTest.FixedClockConfiguration.class
})
class ProgramAccessAuthorizationIntegrationTest {

    private static final Instant NOW =
            Instant.parse(
                    "2026-10-01T12:00:00Z"
            );

    @Autowired
    private ProgramAccessAuthorizationService
            authorizationService;

    @Autowired
    private ProgramAccessRepository
            programAccessRepository;

    @Autowired
    private UserRepository
            userRepository;

    @Autowired
    private ProgramRepository
            programRepository;

    @Autowired
    private PasswordResetTokenRepository
            passwordResetTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        programAccessRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        programRepository.deleteAll();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    NOW,
                    ZoneOffset.UTC
            );
        }
    }

    @Test
    void shouldRecognizeActiveProgramAccess() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        createAccess(
                user,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                Instant.parse(
                        "2027-03-01T12:00:00Z"
                )
        );

        assertTrue(
                authorizationService
                        .hasActiveAccess(
                                user.getId(),
                                program.getId()
                        )
        );
    }

    @Test
    void shouldAllowAccessStartingExactlyNow() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        createAccess(
                user,
                program,
                NOW,
                Instant.parse(
                        "2027-03-01T12:00:00Z"
                )
        );

        assertTrue(
                authorizationService
                        .hasActiveAccess(
                                user.getId(),
                                program.getId()
                        )
        );
    }

    @Test
    void shouldRejectProgramAccessThatHasNotStartedYet() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        createAccess(
                user,
                program,
                Instant.parse(
                        "2026-10-02T12:00:00Z"
                ),
                null
        );

        assertFalse(
                authorizationService
                        .hasActiveAccess(
                                user.getId(),
                                program.getId()
                        )
        );
    }

    @Test
    void shouldAllowProgramAccessWithoutExpiration() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Private Coaching"
                );

        createAccess(
                user,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        assertTrue(
                authorizationService
                        .hasActiveAccess(
                                user.getId(),
                                program.getId()
                        )
        );
    }

    @Test
    void shouldAllowAccessThatExpiresAfterNow() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        createAccess(
                user,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                Instant.parse(
                        "2026-10-01T12:00:01Z"
                )
        );

        assertTrue(
                authorizationService
                        .hasActiveAccess(
                                user.getId(),
                                program.getId()
                        )
        );
    }

    @Test
    void shouldRejectAccessExpiringExactlyNow() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        createAccess(
                user,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                NOW
        );

        assertFalse(
                authorizationService
                        .hasActiveAccess(
                                user.getId(),
                                program.getId()
                        )
        );
    }

    @Test
    void shouldRejectExpiredProgramAccess() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        createAccess(
                user,
                program,
                Instant.parse(
                        "2026-01-01T12:00:00Z"
                ),
                Instant.parse(
                        "2026-09-30T12:00:00Z"
                )
        );

        assertFalse(
                authorizationService
                        .hasActiveAccess(
                                user.getId(),
                                program.getId()
                        )
        );
    }

    @Test
    void shouldRejectRevokedProgramAccess() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        ProgramAccess access =
                createAccess(
                        user,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        Instant.parse(
                                "2027-03-01T12:00:00Z"
                        )
                );

        access.revoke(
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                )
        );

        programAccessRepository.saveAndFlush(
                access
        );

        assertFalse(
                authorizationService
                        .hasActiveAccess(
                                user.getId(),
                                program.getId()
                        )
        );
    }

    @Test
    void shouldAllowNewActiveAccessWhenOlderAccessIsExpired() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        createAccess(
                user,
                program,
                Instant.parse(
                        "2026-01-01T12:00:00Z"
                ),
                Instant.parse(
                        "2026-06-01T12:00:00Z"
                )
        );

        ProgramAccess activeAccess =
                createAccess(
                        user,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        Instant.parse(
                                "2027-03-01T12:00:00Z"
                        )
                );

        Optional<ProgramAccess> result =
                authorizationService
                        .findActiveAccess(
                                user.getId(),
                                program.getId()
                        );

        assertTrue(
                result.isPresent()
        );

        assertEquals(
                activeAccess.getId(),
                result.orElseThrow().getId()
        );
    }

    @Test
    void shouldReturnMostRecentlyStartedActiveAccess() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        createAccess(
                user,
                program,
                Instant.parse(
                        "2026-08-01T12:00:00Z"
                ),
                null
        );

        ProgramAccess newestAccess =
                createAccess(
                        user,
                        program,
                        Instant.parse(
                                "2026-09-15T12:00:00Z"
                        ),
                        null
                );

        ProgramAccess result =
                authorizationService
                        .findActiveAccess(
                                user.getId(),
                                program.getId()
                        )
                        .orElseThrow();

        assertEquals(
                newestAccess.getId(),
                result.getId()
        );
    }

    @Test
    void shouldNotUseAnotherUsersProgramAccess() {
        User userWithAccess =
                createUser(
                        "member-a@example.com"
                );

        User otherUser =
                createUser(
                        "member-b@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        createAccess(
                userWithAccess,
                program,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        assertFalse(
                authorizationService
                        .hasActiveAccess(
                                otherUser.getId(),
                                program.getId()
                        )
        );
    }

    @Test
    void shouldNotUseAccessFromAnotherProgram() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program programWithAccess =
                createProgram(
                        "Program A"
                );

        Program otherProgram =
                createProgram(
                        "Program B"
                );

        createAccess(
                user,
                programWithAccess,
                Instant.parse(
                        "2026-09-01T12:00:00Z"
                ),
                null
        );

        assertFalse(
                authorizationService
                        .hasActiveAccess(
                                user.getId(),
                                otherProgram.getId()
                        )
        );
    }

    @Test
    void shouldReturnEmptyWhenActiveAccessDoesNotExist() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Optional<ProgramAccess> result =
                authorizationService
                        .findActiveAccess(
                                user.getId(),
                                program.getId()
                        );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldRequireAndReturnActiveProgramAccess() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        ProgramAccess access =
                createAccess(
                        user,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        ProgramAccess result =
                authorizationService
                        .requireActiveAccess(
                                user.getId(),
                                program.getId()
                        );

        assertEquals(
                access.getId(),
                result.getId()
        );
    }

    @Test
    void shouldThrowWhenActiveProgramAccessDoesNotExist() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        assertThrows(
                ProgramAccessDeniedException.class,
                () ->
                        authorizationService
                                .requireActiveAccess(
                                        user.getId(),
                                        program.getId()
                                )
        );
    }

    @Test
    void shouldThrowWhenRequiredProgramAccessIsRevoked() {
        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        ProgramAccess access =
                createAccess(
                        user,
                        program,
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        null
                );

        access.revoke(
                Instant.parse(
                        "2026-09-20T12:00:00Z"
                )
        );

        programAccessRepository.saveAndFlush(
                access
        );

        assertThrows(
                ProgramAccessDeniedException.class,
                () ->
                        authorizationService
                                .requireActiveAccess(
                                        user.getId(),
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
                        "encoded-password",
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

    private ProgramAccess createAccess(
            User user,
            Program program,
            Instant startsAt,
            Instant expiresAt
    ) {
        return programAccessRepository.saveAndFlush(
                new ProgramAccess(
                        user,
                        program,
                        startsAt,
                        expiresAt
                )
        );
    }
}