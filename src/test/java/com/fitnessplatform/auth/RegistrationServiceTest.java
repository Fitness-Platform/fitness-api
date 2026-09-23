package com.fitnessplatform.auth;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void shouldNormalizeEmailEncodePasswordAndCreateUserRole() {
        RegisterRequest request = new RegisterRequest(
                "Samuel@Example.com",
                "StrongPassword123!"
        );

        when(userRepository.existsByEmail("samuel@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("StrongPassword123!"))
                .thenReturn("encoded-password");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.register(request);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).saveAndFlush(userCaptor.capture());

        User user = userCaptor.getValue();

        assertEquals("samuel@example.com", user.getEmail());
        assertEquals("encoded-password", user.getPasswordHash());
        assertEquals(UserRole.USER, user.getRole());
    }

    @Test
    void shouldRejectAlreadyRegisteredEmail() {
        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "StrongPassword123!"
        );

        when(userRepository.existsByEmail("existing@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> registrationService.register(request)
        );

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }
}