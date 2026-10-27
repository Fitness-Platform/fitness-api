package com.fitnessplatform.user;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccessRepository;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.purchase.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgramAccessRepository programAccessRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @BeforeEach
    void cleanDatabase() {
        programAccessRepository.deleteAll();
        purchaseRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
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
    @Test
    void shouldOrderUsersByNewestThenIdForEqualTimestamps() {
        User older = saveUserAt("older@example.com", "2026-01-01T00:00:00Z");
        User first = saveUserAt("first@example.com", "2026-02-01T00:00:00Z");
        User second = saveUserAt("second@example.com", "2026-02-01T00:00:00Z");
        List<User> ordered = userRepository.findAllByOrderByCreatedAtDescIdAsc();

        assertEquals(older.getId(), ordered.get(2).getId());
        assertEquals(
                List.of(first.getId().toString(), second.getId().toString()).stream().sorted().toList(),
                ordered.subList(0, 2).stream().map(user -> user.getId().toString()).toList()
        );
    }

    @Test
    void shouldSearchCaseInsensitivelyInDeterministicOrder() {
        User older = saveUserAt("First@Example.com", "2026-01-01T00:00:00Z");
        User newer = saveUserAt("second@example.COM", "2026-02-01T00:00:00Z");
        saveUserAt("other@elsewhere.test", "2026-03-01T00:00:00Z");

        assertEquals(List.of(newer.getId(), older.getId()),
                userRepository.findByEmailContainingIgnoreCaseOrderByCreatedAtDescIdAsc("EXAMPLE")
                        .stream().map(User::getId).toList());
    }

    @Test
    void shouldTreatSearchWildcardsAsLiteralCharacters() {
        User literal = saveUserAt("literal%_@example.com", "2026-01-01T00:00:00Z");
        saveUserAt("other@example.com", "2026-02-01T00:00:00Z");

        assertEquals(List.of(literal.getId()),
                userRepository.findByEmailContainingIgnoreCaseOrderByCreatedAtDescIdAsc("%_")
                        .stream().map(User::getId).toList());
    }

    private User saveUserAt(String email, String createdAt) {
        User user = new User(email, "encoded-password", UserRole.USER);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse(createdAt));
        return userRepository.saveAndFlush(user);
    }
}