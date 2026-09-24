package com.fitnessplatform.auth.passwordreset;

import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                tokenRepository,
                passwordEncoder,
                Duration.ofMinutes(30)
        );
    }

    @Test
    void shouldCreatePasswordResetTokenForExistingUser() {
        User user = new User(
                "samuel@example.com",
                "encoded-password",
                UserRole.USER
        );

        when(userRepository.findByEmail("samuel@example.com"))
                .thenReturn(Optional.of(user));

        String rawToken = passwordResetService
                .requestReset("  Samuel@Example.com  ");

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        verify(userRepository)
                .findByEmail("samuel@example.com");

        verify(tokenRepository)
                .deleteByUserId(user.getId());

        ArgumentCaptor<PasswordResetToken> captor =
                ArgumentCaptor.forClass(
                        PasswordResetToken.class
                );

        verify(tokenRepository)
                .save(captor.capture());

        PasswordResetToken storedToken =
                captor.getValue();

        assertNotEquals(
                rawToken,
                storedToken.getTokenHash()
        );

        assertEquals(
                passwordResetService.hashToken(rawToken),
                storedToken.getTokenHash()
        );
    }

    @Test
    void shouldNotCreateTokenWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        String rawToken = passwordResetService
                .requestReset("unknown@example.com");

        assertNull(rawToken);

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void shouldResetPasswordWithValidToken() {
        User user = new User(
                "samuel@example.com",
                "old-password-hash",
                UserRole.USER
        );

        String rawToken = "valid-reset-token";

        String tokenHash =
                passwordResetService.hashToken(rawToken);

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        user,
                        tokenHash,
                        Instant.now().plusSeconds(300)
                );

        when(tokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(resetToken));

        when(passwordEncoder.encode("NewStrongPassword123!"))
                .thenReturn("new-password-hash");

        passwordResetService.resetPassword(
                rawToken,
                "NewStrongPassword123!"
        );

        assertEquals(
                "new-password-hash",
                user.getPasswordHash()
        );

        verify(passwordEncoder)
                .encode("NewStrongPassword123!");

        verify(tokenRepository)
                .delete(resetToken);
    }

    @Test
    void shouldRejectUnknownPasswordResetToken() {
        String rawToken = "unknown-token";

        String tokenHash =
                passwordResetService.hashToken(rawToken);

        when(tokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> passwordResetService.resetPassword(
                        rawToken,
                        "NewStrongPassword123!"
                )
        );

        verifyNoInteractions(passwordEncoder);

        verify(tokenRepository, never())
                .delete(any());
    }

    @Test
    void shouldRejectExpiredPasswordResetToken() {
        User user = new User(
                "samuel@example.com",
                "old-password-hash",
                UserRole.USER
        );

        String rawToken = "expired-token";

        String tokenHash =
                passwordResetService.hashToken(rawToken);

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        user,
                        tokenHash,
                        Instant.now().minusSeconds(60)
                );

        when(tokenRepository.findByTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(resetToken));

        assertThrows(
                InvalidPasswordResetTokenException.class,
                () -> passwordResetService.resetPassword(
                        rawToken,
                        "NewStrongPassword123!"
                )
        );

        assertEquals(
                "old-password-hash",
                user.getPasswordHash()
        );

        verifyNoInteractions(passwordEncoder);

        verify(tokenRepository, never())
                .delete(any());
    }
}