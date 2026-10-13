package com.fitnessplatform.purchase;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PurchaseIntegrationTest {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private PasswordResetTokenRepository
            passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        purchaseRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        programRepository.deleteAll();
    }

    @AfterEach
    void cleanPurchasesAfterTest() {
        purchaseRepository.deleteAll();
    }

    @Test
    void shouldPersistPurchase() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                purchaseRepository.saveAndFlush(
                        new Purchase(
                                user,
                                program,
                                4999L,
                                "USD"
                        )
                );

        Purchase persisted =
                purchaseRepository
                        .findById(
                                purchase.getId()
                        )
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
                4999L,
                persisted.getAmountCents()
        );

        assertEquals(
                "USD",
                persisted.getCurrency()
        );

        assertEquals(
                PurchaseStatus.PENDING,
                persisted.getStatus()
        );

        assertNull(
                persisted.getPaidAt()
        );

        assertNull(
                persisted.getStripeCheckoutSessionId()
        );

        assertNotNull(
                persisted.getCreatedAt()
        );

        assertNotNull(
                persisted.getUpdatedAt()
        );
    }

    @Test
    void shouldPersistStripeCheckoutSessionId() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        4999L,
                        "USD"
                );

        purchase.attachStripeCheckoutSession(
                "cs_test_example"
        );

        purchase =
                purchaseRepository.saveAndFlush(
                        purchase
                );

        Purchase persisted =
                purchaseRepository
                        .findById(
                                purchase.getId()
                        )
                        .orElseThrow();

        assertEquals(
                "cs_test_example",
                persisted.getStripeCheckoutSessionId()
        );
    }

    @Test
    void shouldMarkPendingPurchaseAsPaid() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        8900L,
                        "USD"
                );

        purchase.attachStripeCheckoutSession(
                "cs_test_paid"
        );

        purchase =
                purchaseRepository.saveAndFlush(
                        purchase
                );

        Instant paidAt =
                Instant.parse(
                        "2026-10-09T12:00:00Z"
                );

        purchase.markPaid(
                paidAt,
                "pi_test_123"
        );

        purchaseRepository.saveAndFlush(
                purchase
        );

        Purchase persisted =
                purchaseRepository
                        .findById(
                                purchase.getId()
                        )
                        .orElseThrow();

        assertEquals(
                PurchaseStatus.PAID,
                persisted.getStatus()
        );

        assertEquals(
                paidAt,
                persisted.getPaidAt()
        );

        assertEquals(
                "pi_test_123",
                persisted.getStripePaymentIntentId()
        );

        assertEquals(
                "cs_test_paid",
                persisted.getStripeCheckoutSessionId()
        );
    }

    @Test
    void shouldRejectMarkingNonPendingPurchaseAsPaid() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        8900L,
                        "USD"
                );

        purchase.markPaid(
                Instant.parse(
                        "2026-10-09T12:00:00Z"
                ),
                "pi_test_123"
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        purchase.markPaid(
                                Instant.parse(
                                        "2026-10-09T13:00:00Z"
                                ),
                                "pi_test_456"
                        )
        );
    }

    @Test
    void shouldAllowMultiplePurchasesForSameUserAndProgram() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase firstPurchase =
                purchaseRepository.saveAndFlush(
                        new Purchase(
                                user,
                                program,
                                4999L,
                                "USD"
                        )
                );

        Purchase secondPurchase =
                purchaseRepository.saveAndFlush(
                        new Purchase(
                                user,
                                program,
                                5999L,
                                "USD"
                        )
                );

        assertEquals(
                2,
                purchaseRepository.count()
        );

        assertTrue(
                purchaseRepository.existsById(
                        firstPurchase.getId()
                )
        );

        assertTrue(
                purchaseRepository.existsById(
                        secondPurchase.getId()
                )
        );
    }


    @Test
    void shouldRejectPurchaseWithNonPositiveAmount() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        0L,
                        "USD"
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        purchaseRepository.saveAndFlush(
                                purchase
                        )
        );
    }

    @Test
    void shouldRejectPurchaseWithInvalidCurrencyLength() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        Purchase purchase =
                new Purchase(
                        user,
                        program,
                        4999L,
                        "US"
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        purchaseRepository.saveAndFlush(
                                purchase
                        )
        );
    }

    @Test
    void shouldPreventDeletingUserReferencedByPurchase() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        purchaseRepository.saveAndFlush(
                new Purchase(
                        user,
                        program,
                        4999L,
                        "USD"
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    userRepository.delete(
                            user
                    );

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
    void shouldPreventDeletingProgramReferencedByPurchase() {

        User user =
                createUser(
                        "member@example.com"
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        purchaseRepository.saveAndFlush(
                new Purchase(
                        user,
                        program,
                        4999L,
                        "USD"
                )
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> {
                    programRepository.delete(
                            program
                    );

                    programRepository.flush();
                }
        );

        assertTrue(
                programRepository.existsById(
                        program.getId()
                )
        );
    }

    @Test
    void shouldMarkPendingPurchaseAsExpired() {
        User user = createUser("member@example.com");
        Program program = createProgram("Strength Program");

        Purchase purchase = new Purchase(
                user,
                program,
                8900L,
                "USD"
        );

        purchase.attachStripeCheckoutSession("cs_test_expired");

        purchase = purchaseRepository.saveAndFlush(purchase);

        purchase.markExpired();

        purchaseRepository.saveAndFlush(purchase);

        Purchase persisted = purchaseRepository
                .findById(purchase.getId())
                .orElseThrow();

        assertEquals(
                PurchaseStatus.EXPIRED,
                persisted.getStatus()
        );

        assertNull(persisted.getPaidAt());
        assertNull(persisted.getStripePaymentIntentId());

        assertEquals(
                "cs_test_expired",
                persisted.getStripeCheckoutSessionId()
        );
    }

    @Test
    void shouldRejectExpiringPaidPurchase() {
        User user = createUser("member@example.com");
        Program program = createProgram("Strength Program");

        Purchase purchase = new Purchase(
                user,
                program,
                8900L,
                "USD"
        );

        Instant paidAt = Instant.parse(
                "2026-10-09T12:00:00Z"
        );

        purchase.markPaid(
                paidAt,
                "pi_test_123"
        );

        assertThrows(
                IllegalStateException.class,
                purchase::markExpired
        );

        assertEquals(
                PurchaseStatus.PAID,
                purchase.getStatus()
        );

        assertEquals(paidAt, purchase.getPaidAt());

        assertEquals(
                "pi_test_123",
                purchase.getStripePaymentIntentId()
        );
    }

    @Test
    void shouldRejectMarkingExpiredPurchaseAsPaid() {
        User user = createUser("member@example.com");
        Program program = createProgram("Strength Program");

        Purchase purchase = new Purchase(
                user,
                program,
                8900L,
                "USD"
        );

        purchase.markExpired();

        assertThrows(
                IllegalStateException.class,
                () -> purchase.markPaid(
                        Instant.parse("2026-10-09T12:00:00Z"),
                        "pi_test_123"
                )
        );

        assertEquals(
                PurchaseStatus.EXPIRED,
                purchase.getStatus()
        );

        assertNull(purchase.getPaidAt());
        assertNull(purchase.getStripePaymentIntentId());
    }

    @Test
    void shouldAllowRepeatedExpirationWithoutChanges() {
        User user = createUser("member@example.com");
        Program program = createProgram("Strength Program");

        Purchase purchase = new Purchase(
                user,
                program,
                8900L,
                "USD"
        );

        purchase = purchaseRepository.saveAndFlush(purchase);

        purchase.markExpired();
        purchase.markExpired();

        purchaseRepository.saveAndFlush(purchase);

        Purchase persisted = purchaseRepository
                .findById(purchase.getId())
                .orElseThrow();

        assertEquals(
                PurchaseStatus.EXPIRED,
                persisted.getStatus()
        );

        assertNull(persisted.getPaidAt());
        assertNull(persisted.getStripePaymentIntentId());
    }

    @Test
    void shouldListOnlyPurchasesBelongingToUser() {
        User member = createUser("member@example.com");
        User anotherMember = createUser("another@example.com");

        Program program = createProgram("Strength Program");

        Purchase firstPurchase = purchaseRepository.saveAndFlush(
                new Purchase(member, program, 8900L, "USD")
        );

        Purchase secondPurchase = purchaseRepository.saveAndFlush(
                new Purchase(member, program, 9900L, "USD")
        );

        Purchase anotherMemberPurchase = purchaseRepository.saveAndFlush(
                new Purchase(anotherMember, program, 7900L, "USD")
        );

        List<Purchase> purchases =
                purchaseRepository.findAllByUserIdForMember(member.getId());

        assertEquals(2, purchases.size());

        assertTrue(
                purchases.stream()
                        .allMatch(purchase ->
                                purchase.getUser().getId().equals(member.getId()))
        );

        assertTrue(
                purchases.stream()
                        .anyMatch(purchase ->
                                purchase.getId().equals(firstPurchase.getId()))
        );

        assertTrue(
                purchases.stream()
                        .anyMatch(purchase ->
                                purchase.getId().equals(secondPurchase.getId()))
        );

        assertFalse(
                purchases.stream()
                        .anyMatch(purchase ->
                                purchase.getId().equals(anotherMemberPurchase.getId()))
        );
    }

    @Test
    void shouldListPurchasesOrderedByCreationDateDescending()
            throws InterruptedException {

        User member = createUser("member@example.com");
        Program program = createProgram("Strength Program");

        Purchase olderPurchase = purchaseRepository.saveAndFlush(
                new Purchase(member, program, 8900L, "USD")
        );

        Thread.sleep(20);

        Purchase newerPurchase = purchaseRepository.saveAndFlush(
                new Purchase(member, program, 9900L, "USD")
        );

        List<Purchase> purchases =
                purchaseRepository.findAllByUserIdForMember(member.getId());

        assertEquals(2, purchases.size());

        assertEquals(newerPurchase.getId(), purchases.get(0).getId());
        assertEquals(olderPurchase.getId(), purchases.get(1).getId());

        assertTrue(
                !purchases.get(0).getCreatedAt()
                        .isBefore(purchases.get(1).getCreatedAt())
        );
    }

    @Test
    void shouldFindPurchaseByIdAndOwner() {
        User member = createUser("member@example.com");
        Program program = createProgram("Strength Program");

        Purchase purchase = purchaseRepository.saveAndFlush(
                new Purchase(member, program, 8900L, "USD")
        );

        Optional<Purchase> result =
                purchaseRepository.findByIdAndUserIdForMember(
                        purchase.getId(),
                        member.getId()
                );

        assertTrue(result.isPresent());

        Purchase found = result.orElseThrow();

        assertEquals(purchase.getId(), found.getId());
        assertEquals(member.getId(), found.getUser().getId());
        assertEquals(program.getId(), found.getProgram().getId());
        assertEquals("Strength Program", found.getProgram().getName());
        assertEquals(8900L, found.getAmountCents());
        assertEquals("USD", found.getCurrency());
        assertEquals(PurchaseStatus.PENDING, found.getStatus());
    }

    @Test
    void shouldNotFindPurchaseBelongingToAnotherUser() {
        User owner = createUser("owner@example.com");
        User anotherMember = createUser("another@example.com");

        Program program = createProgram("Strength Program");

        Purchase purchase = purchaseRepository.saveAndFlush(
                new Purchase(owner, program, 8900L, "USD")
        );

        Optional<Purchase> result =
                purchaseRepository.findByIdAndUserIdForMember(
                        purchase.getId(),
                        anotherMember.getId()
                );

        assertTrue(result.isEmpty());

        assertTrue(purchaseRepository.existsById(purchase.getId()));
    }

    @Test
    void shouldListPurchasesFromDifferentUsersForAdmin() {
        User firstUser = createUser("first@example.com");
        User secondUser = createUser("second@example.com");

        Program program = createProgram("Strength Program");

        Purchase firstPurchase = purchaseRepository.saveAndFlush(
                new Purchase(firstUser, program, 8900L, "USD")
        );

        Purchase secondPurchase = purchaseRepository.saveAndFlush(
                new Purchase(secondUser, program, 9900L, "USD")
        );

        List<Purchase> purchases = purchaseRepository.findAllForAdmin();

        assertEquals(2, purchases.size());

        assertTrue(
                purchases.stream()
                        .anyMatch(purchase ->
                                purchase.getId().equals(firstPurchase.getId()))
        );

        assertTrue(
                purchases.stream()
                        .anyMatch(purchase ->
                                purchase.getId().equals(secondPurchase.getId()))
        );

        assertTrue(
                purchases.stream()
                        .anyMatch(purchase ->
                                purchase.getUser().getId().equals(firstUser.getId()))
        );

        assertTrue(
                purchases.stream()
                        .anyMatch(purchase ->
                                purchase.getUser().getId().equals(secondUser.getId()))
        );
    }

    @Test
    void shouldFindPurchaseByIdForAdmin() {
        User buyer = createUser("buyer@example.com");
        Program program = createProgram("Strength Program");

        Purchase purchase = purchaseRepository.saveAndFlush(
                new Purchase(buyer, program, 8900L, "USD")
        );

        Optional<Purchase> result =
                purchaseRepository.findByIdForAdmin(purchase.getId());

        assertTrue(result.isPresent());

        Purchase found = result.orElseThrow();

        assertEquals(purchase.getId(), found.getId());
        assertEquals(buyer.getId(), found.getUser().getId());
        assertEquals(program.getId(), found.getProgram().getId());
        assertEquals("Strength Program", found.getProgram().getName());
        assertEquals(8900L, found.getAmountCents());
        assertEquals("USD", found.getCurrency());
        assertEquals(PurchaseStatus.PENDING, found.getStatus());
    }

    @Test
    void shouldReturnEmptyWhenAdminPurchaseDoesNotExist() {
        UUID purchaseId = UUID.randomUUID();

        Optional<Purchase> result =
                purchaseRepository.findByIdForAdmin(purchaseId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldListAdminPurchasesOrderedByCreationDateDescending() {
        User buyer = createUser("buyer@example.com");
        Program program = createProgram("Strength Program");

        Purchase olderPurchase = purchaseRepository.saveAndFlush(
                new Purchase(buyer, program, 8900L, "USD")
        );

        Purchase newerPurchase = purchaseRepository.saveAndFlush(
                new Purchase(buyer, program, 9900L, "USD")
        );

        jdbcTemplate.update(
                "UPDATE purchases SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(
                        Instant.parse("2026-01-01T12:00:00Z")
                ),
                olderPurchase.getId()
        );

        jdbcTemplate.update(
                "UPDATE purchases SET created_at = ? WHERE id = ?",
                java.sql.Timestamp.from(
                        Instant.parse("2026-02-01T12:00:00Z")
                ),
                newerPurchase.getId()
        );

        List<Purchase> purchases =
                purchaseRepository.findAllForAdmin();

        assertEquals(2, purchases.size());

        assertEquals(
                newerPurchase.getId(),
                purchases.get(0).getId()
        );

        assertEquals(
                olderPurchase.getId(),
                purchases.get(1).getId()
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