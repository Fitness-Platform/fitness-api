package com.fitnessplatform.purchase;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/purchases")
public class AdminPurchaseController {

    private final AdminPurchaseService adminPurchaseService;

    public AdminPurchaseController(
            AdminPurchaseService adminPurchaseService
    ) {
        this.adminPurchaseService = adminPurchaseService;
    }

    @GetMapping
    public List<AdminPurchaseResponse> listPurchases() {
        return adminPurchaseService.listPurchases();
    }

    @GetMapping("/{purchaseId}")
    public AdminPurchaseResponse getPurchase(
            @PathVariable UUID purchaseId
    ) {
        return adminPurchaseService.getPurchase(purchaseId);
    }
}