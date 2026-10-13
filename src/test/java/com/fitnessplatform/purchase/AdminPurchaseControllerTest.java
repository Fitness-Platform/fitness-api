package com.fitnessplatform.purchase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminPurchaseControllerTest {

    private AdminPurchaseService adminPurchaseService;
    private AdminPurchaseController controller;

    private UUID purchaseId;

    @BeforeEach
    void setUp() {
        adminPurchaseService = mock(AdminPurchaseService.class);
        controller = new AdminPurchaseController(adminPurchaseService);

        purchaseId = UUID.randomUUID();
    }

    @Test
    void shouldListPurchases() {
        AdminPurchaseResponse response = createResponse();

        when(adminPurchaseService.listPurchases())
                .thenReturn(List.of(response));

        List<AdminPurchaseResponse> result =
                controller.listPurchases();

        assertEquals(1, result.size());
        assertEquals(purchaseId, result.getFirst().purchaseId());

        verify(adminPurchaseService).listPurchases();
    }

    @Test
    void shouldReturnEmptyListWhenNoPurchasesExist() {
        when(adminPurchaseService.listPurchases())
                .thenReturn(List.of());

        List<AdminPurchaseResponse> result =
                controller.listPurchases();

        assertTrue(result.isEmpty());

        verify(adminPurchaseService).listPurchases();
    }

    @Test
    void shouldGetPurchaseDetails() {
        AdminPurchaseResponse response = createResponse();

        when(adminPurchaseService.getPurchase(purchaseId))
                .thenReturn(response);

        AdminPurchaseResponse result =
                controller.getPurchase(purchaseId);

        assertEquals(purchaseId, result.purchaseId());
        assertEquals(PurchaseStatus.PAID, result.status());

        verify(adminPurchaseService).getPurchase(purchaseId);
    }

    private AdminPurchaseResponse createResponse() {
        return new AdminPurchaseResponse(
                purchaseId,
                UUID.randomUUID(),
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