package com.fitnessplatform.user;

import com.fitnessplatform.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldPersistAndFindUserByEmail() {
        User user = new User(
                "samuel@example.com",
                "encoded-password",
                UserRole.USER
        );

        User savedUser = userRepository.saveAndFlush(user);

        Optional<User> foundUser = userRepository.findByEmail(
                "samuel@example.com"
        );

        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getCreatedAt());

        assertTrue(foundUser.isPresent());
        assertEquals("samuel@example.com", foundUser.get().getEmail());
        assertEquals("encoded-password", foundUser.get().getPasswordHash());
        assertEquals(UserRole.USER, foundUser.get().getRole());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        User firstUser = new User(
                "duplicate@example.com",
                "encoded-password",
                UserRole.USER
        );

        User secondUser = new User(
                "duplicate@example.com",
                "another-password",
                UserRole.USER
        );

        userRepository.saveAndFlush(firstUser);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(secondUser)
        );
    }
}