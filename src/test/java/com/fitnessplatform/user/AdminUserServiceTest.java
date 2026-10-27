package com.fitnessplatform.user;

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
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository);
    }

    @Test
    void shouldListUsersWithoutEmailFilter() {
        List<User> users = List.of(new User("admin@example.com", "hash", UserRole.ADMIN));
        when(userRepository.findAllByOrderByCreatedAtDescIdAsc()).thenReturn(users);

        assertEquals(users, service.findAll(null));
        verify(userRepository).findAllByOrderByCreatedAtDescIdAsc();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldTreatBlankEmailAsNoFilter() {
        when(userRepository.findAllByOrderByCreatedAtDescIdAsc()).thenReturn(List.of());

        assertTrue(service.findAll(" \t ").isEmpty());
        verify(userRepository).findAllByOrderByCreatedAtDescIdAsc();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldStripSearchAndUseCaseInsensitiveQuery() {
        List<User> users = List.of(new User("member@example.com", "hash", UserRole.USER));
        when(userRepository.findByEmailContainingIgnoreCaseOrderByCreatedAtDescIdAsc("EXAMPLE"))
                .thenReturn(users);

        assertEquals(users, service.findAll("  EXAMPLE  "));
        verify(userRepository).findByEmailContainingIgnoreCaseOrderByCreatedAtDescIdAsc("EXAMPLE");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void shouldReturnExistingUser() {
        UUID id = UUID.randomUUID();
        User user = new User("member@example.com", "hash", UserRole.USER);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertSame(user, service.findById(id));
    }

    @Test
    void shouldRejectUnknownUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertEquals("User not found: " + id,
                assertThrows(UserNotFoundException.class, () -> service.findById(id)).getMessage());
    }
}
