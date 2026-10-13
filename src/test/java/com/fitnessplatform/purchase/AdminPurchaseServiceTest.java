package com.fitnessplatform.purchase;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private AdminPurchaseService service;

    private UUID purchaseId;
    private UUID userId;
    private UUID programId;

    @BeforeEach
    void setUp() {
        purchaseId = UUID.randomUUID();
        userId = UUID.randomUUID();
        programId = UUID.randomUUID();
    }

    @Test
    void shouldReturnEmptyListWhenNoPurchasesExist() {
        when(purchaseRepository.findAllForAdmin())
                .thenReturn(List.of());

        List<AdminPurchaseResponse> result = service.listPurchases();

        assertTrue(result.isEmpty());

        verify(purchaseRepository).findAllForAdmin();
    }

    @Test
    void shouldReturnPurchaseDetailsForAdmin() {
        Purchase purchase = createPurchaseMock();

        when(purchaseRepository.findByIdForAdmin(purchaseId))
                .thenReturn(Optional.of(purchase));

        AdminPurchaseResponse response =
                service.getPurchase(purchaseId);

        assertAll(
                () -> assertEquals(purchaseId, response.purchaseId()),
                () -> assertEquals(userId, response.userId()),
                () -> assertEquals(programId, response.programId()),
                () -> assertEquals("Strength Program", response.programName()),
                () -> assertEquals(PurchaseStatus.PAID, response.status()),
                () -> assertEquals(8900L, response.amountCents()),
                () -> assertEquals("USD", response.currency()),
                () -> assertEquals(
                        Instant.parse("2026-10-13T12:00:00Z"),
                        response.createdAt()
                ),
                () -> assertEquals(
                        Instant.parse("2026-10-13T12:05:00Z"),
                        response.paidAt()
                )
        );

        verify(purchaseRepository).findByIdForAdmin(purchaseId);
    }

    @Test
    void shouldMapPurchasesFromDifferentUsers() {
        Purchase firstPurchase = createPurchaseMock();

        UUID secondPurchaseId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        User secondUser = mock(User.class);
        Purchase secondPurchase = mock(Purchase.class);

        when(secondUser.getId()).thenReturn(secondUserId);
        when(secondPurchase.getId()).thenReturn(secondPurchaseId);
        when(secondPurchase.getUser()).thenReturn(secondUser);

        Program secondProgram = mock(Program.class);
        when(secondProgram.getId()).thenReturn(programId);
        when(secondProgram.getName()).thenReturn("Strength Program");

        when(secondPurchase.getProgram()).thenReturn(secondProgram);
        when(secondPurchase.getStatus()).thenReturn(PurchaseStatus.PENDING);
        when(secondPurchase.getAmountCents()).thenReturn(9900L);
        when(secondPurchase.getCurrency()).thenReturn("USD");

        when(purchaseRepository.findAllForAdmin())
                .thenReturn(List.of(firstPurchase, secondPurchase));

        List<AdminPurchaseResponse> responses =
                service.listPurchases();

        assertEquals(2, responses.size());

        assertEquals(userId, responses.get(0).userId());
        assertEquals(secondUserId, responses.get(1).userId());

        assertEquals(PurchaseStatus.PAID, responses.get(0).status());
        assertEquals(PurchaseStatus.PENDING, responses.get(1).status());
    }

    @Test
    void shouldThrowNotFoundWhenPurchaseDoesNotExist() {
        when(purchaseRepository.findByIdForAdmin(purchaseId))
                .thenReturn(Optional.empty());

        assertThrows(
                AdminPurchaseNotFoundException.class,
                () -> service.getPurchase(purchaseId)
        );

        verify(purchaseRepository).findByIdForAdmin(purchaseId);
    }

    private Purchase createPurchaseMock() {
        User user = mock(User.class);
        Program program = mock(Program.class);
        Purchase purchase = mock(Purchase.class);

        when(user.getId()).thenReturn(userId);

        when(program.getId()).thenReturn(programId);
        when(program.getName()).thenReturn("Strength Program");

        when(purchase.getId()).thenReturn(purchaseId);
        when(purchase.getUser()).thenReturn(user);
        when(purchase.getProgram()).thenReturn(program);
        when(purchase.getStatus()).thenReturn(PurchaseStatus.PAID);
        when(purchase.getAmountCents()).thenReturn(8900L);
        when(purchase.getCurrency()).thenReturn("USD");
        when(purchase.getCreatedAt())
                .thenReturn(Instant.parse("2026-10-13T12:00:00Z"));
        when(purchase.getPaidAt())
                .thenReturn(Instant.parse("2026-10-13T12:05:00Z"));

        return purchase;
    }
}