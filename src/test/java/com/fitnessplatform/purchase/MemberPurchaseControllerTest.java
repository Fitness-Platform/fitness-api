package com.fitnessplatform.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MemberPurchaseControllerTest {

    private MemberPurchaseService memberPurchaseService;
    private MemberPurchaseController controller;

    private UUID userId;
    private UUID purchaseId;

    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        memberPurchaseService = mock(MemberPurchaseService.class);
        controller = new MemberPurchaseController(memberPurchaseService);

        userId = UUID.randomUUID();
        purchaseId = UUID.randomUUID();

        authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of()
        );
    }

    @Test
    void shouldListPurchasesForAuthenticatedMember() {
        MemberPurchaseResponse response = createResponse();

        when(memberPurchaseService.listPurchases(userId))
                .thenReturn(List.of(response));

        List<MemberPurchaseResponse> result =
                controller.listPurchases(authentication);

        assertEquals(1, result.size());
        assertEquals(purchaseId, result.getFirst().purchaseId());

        verify(memberPurchaseService).listPurchases(userId);
    }

    @Test
    void shouldReturnEmptyListWhenMemberHasNoPurchases() {
        when(memberPurchaseService.listPurchases(userId))
                .thenReturn(List.of());

        List<MemberPurchaseResponse> result =
                controller.listPurchases(authentication);

        assertEquals(0, result.size());

        verify(memberPurchaseService).listPurchases(userId);
    }

    @Test
    void shouldGetPurchaseForAuthenticatedMember() {
        MemberPurchaseResponse response = createResponse();

        when(memberPurchaseService.getPurchase(userId, purchaseId))
                .thenReturn(response);

        MemberPurchaseResponse result =
                controller.getPurchase(purchaseId, authentication);

        assertEquals(purchaseId, result.purchaseId());
        assertEquals(PurchaseStatus.PAID, result.status());

        verify(memberPurchaseService)
                .getPurchase(userId, purchaseId);
    }

    private MemberPurchaseResponse createResponse() {
        return new MemberPurchaseResponse(
                purchaseId,
                UUID.randomUUID(),
                "Strength Program",
                PurchaseStatus.PAID,
                8900L,
                "USD",
                Instant.parse("2026-10-13T12:00:00Z"),
                Instant.parse("2026-10-13T12:05:00Z")
        );
    }
}