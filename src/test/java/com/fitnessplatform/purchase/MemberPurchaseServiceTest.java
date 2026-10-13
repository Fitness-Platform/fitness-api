package com.fitnessplatform.purchase;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRole;
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
class MemberPurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @InjectMocks
    private MemberPurchaseService service;

    private UUID userId;
    private UUID purchaseId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        purchaseId = UUID.randomUUID();
    }

    @Test
    void shouldReturnEmptyListWhenMemberHasNoPurchases() {
        when(purchaseRepository.findAllByUserIdForMember(userId))
                .thenReturn(List.of());

        List<MemberPurchaseResponse> result =
                service.listPurchases(userId);

        assertTrue(result.isEmpty());

        verify(purchaseRepository)
                .findAllByUserIdForMember(userId);
    }

    @Test
    void shouldThrowNotFoundWhenPurchaseDoesNotBelongToMember() {
        when(purchaseRepository.findByIdAndUserIdForMember(
                purchaseId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                MemberPurchaseNotFoundException.class,
                () -> service.getPurchase(userId, purchaseId)
        );

        verify(purchaseRepository)
                .findByIdAndUserIdForMember(purchaseId, userId);
    }

    @Test
    void shouldNotUseUnrestrictedFindById() {
        when(purchaseRepository.findByIdAndUserIdForMember(
                purchaseId,
                userId
        )).thenReturn(Optional.empty());

        assertThrows(
                MemberPurchaseNotFoundException.class,
                () -> service.getPurchase(userId, purchaseId)
        );

        verify(purchaseRepository, never())
                .findById(any(UUID.class));
    }

    @Test
    void shouldReturnPurchaseDetailsForOwner() {
        UUID programId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-13T12:00:00Z");
        Instant paidAt = Instant.parse("2026-10-13T12:05:00Z");

        Program program = mock(Program.class);
        Purchase purchase = mock(Purchase.class);

        when(program.getId()).thenReturn(programId);
        when(program.getName()).thenReturn("Strength Program");

        when(purchase.getId()).thenReturn(purchaseId);
        when(purchase.getProgram()).thenReturn(program);
        when(purchase.getStatus()).thenReturn(PurchaseStatus.PAID);
        when(purchase.getAmountCents()).thenReturn(8900L);
        when(purchase.getCurrency()).thenReturn("USD");
        when(purchase.getCreatedAt()).thenReturn(createdAt);
        when(purchase.getPaidAt()).thenReturn(paidAt);

        when(purchaseRepository.findByIdAndUserIdForMember(
                purchaseId,
                userId
        )).thenReturn(Optional.of(purchase));

        MemberPurchaseResponse response =
                service.getPurchase(userId, purchaseId);

        assertAll(
                () -> assertEquals(purchaseId, response.purchaseId()),
                () -> assertEquals(programId, response.programId()),
                () -> assertEquals("Strength Program", response.programName()),
                () -> assertEquals(PurchaseStatus.PAID, response.status()),
                () -> assertEquals(8900L, response.amountCents()),
                () -> assertEquals("USD", response.currency()),
                () -> assertEquals(createdAt, response.createdAt()),
                () -> assertEquals(paidAt, response.paidAt())
        );

        verify(purchaseRepository)
                .findByIdAndUserIdForMember(purchaseId, userId);

        verify(purchaseRepository, never())
                .findById(any(UUID.class));
    }

    @Test
    void shouldMapMemberPurchasesToResponses() {
        UUID firstPurchaseId = UUID.randomUUID();
        UUID secondPurchaseId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        Program program = mock(Program.class);

        Purchase firstPurchase = mock(Purchase.class);
        Purchase secondPurchase = mock(Purchase.class);

        when(program.getId()).thenReturn(programId);
        when(program.getName()).thenReturn("Strength Program");

        when(firstPurchase.getId()).thenReturn(firstPurchaseId);
        when(firstPurchase.getProgram()).thenReturn(program);
        when(firstPurchase.getStatus()).thenReturn(PurchaseStatus.PAID);
        when(firstPurchase.getAmountCents()).thenReturn(8900L);
        when(firstPurchase.getCurrency()).thenReturn("USD");

        when(secondPurchase.getId()).thenReturn(secondPurchaseId);
        when(secondPurchase.getProgram()).thenReturn(program);
        when(secondPurchase.getStatus()).thenReturn(PurchaseStatus.PENDING);
        when(secondPurchase.getAmountCents()).thenReturn(9900L);
        when(secondPurchase.getCurrency()).thenReturn("USD");

        when(purchaseRepository.findAllByUserIdForMember(userId))
                .thenReturn(List.of(firstPurchase, secondPurchase));

        List<MemberPurchaseResponse> responses =
                service.listPurchases(userId);

        assertEquals(2, responses.size());

        MemberPurchaseResponse first = responses.get(0);
        MemberPurchaseResponse second = responses.get(1);

        assertAll(
                () -> assertEquals(firstPurchaseId, first.purchaseId()),
                () -> assertEquals(PurchaseStatus.PAID, first.status()),
                () -> assertEquals(8900L, first.amountCents()),

                () -> assertEquals(secondPurchaseId, second.purchaseId()),
                () -> assertEquals(PurchaseStatus.PENDING, second.status()),
                () -> assertEquals(9900L, second.amountCents())
        );

        verify(purchaseRepository)
                .findAllByUserIdForMember(userId);
    }
}