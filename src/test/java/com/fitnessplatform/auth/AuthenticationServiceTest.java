package com.fitnessplatform.auth;

import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void shouldAuthenticateWithValidCredentials() {
        User user = new User(
                "samuel@example.com",
                "encoded-password",
                UserRole.USER
        );

        when(userRepository.findByEmail("samuel@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "StrongPassword123!",
                "encoded-password"
        )).thenReturn(true);

        User authenticatedUser = authenticationService.authenticate(
                new LoginRequest(
                        "samuel@example.com",
                        "StrongPassword123!"
                )
        );

        assertSame(user, authenticatedUser);
    }

    @Test
    void shouldNormalizeEmailBeforeAuthentication() {
        User user = new User(
                "samuel@example.com",
                "encoded-password",
                UserRole.USER
        );

        when(userRepository.findByEmail("samuel@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "StrongPassword123!",
                "encoded-password"
        )).thenReturn(true);

        authenticationService.authenticate(
                new LoginRequest(
                        "  Samuel@Example.com  ",
                        "StrongPassword123!"
                )
        );

        verify(userRepository)
                .findByEmail("samuel@example.com");
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.authenticate(
                        new LoginRequest(
                                "unknown@example.com",
                                "Password123!"
                        )
                )
        );

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectIncorrectPassword() {
        User user = new User(
                "samuel@example.com",
                "encoded-password",
                UserRole.USER
        );

        when(userRepository.findByEmail("samuel@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authenticationService.authenticate(
                        new LoginRequest(
                                "samuel@example.com",
                                "wrong-password"
                        )
                )
        );
    }

    @Test
    void shouldReturnAuthenticatedUserById() {
        UUID userId = UUID.randomUUID();

        User user = new User(
                "samuel@example.com",
                "encoded-password",
                UserRole.USER
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        User result =
                authenticationService.getAuthenticatedUser(userId);

        assertSame(user, result);

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldFailWhenAuthenticatedUserNoLongerExists() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> authenticationService
                        .getAuthenticatedUser(userId)
        );
    }
}