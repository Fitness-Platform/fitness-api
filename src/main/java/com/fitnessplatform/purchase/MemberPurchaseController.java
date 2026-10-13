package com.fitnessplatform.purchase;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/purchases")
public class MemberPurchaseController {

    private final MemberPurchaseService memberPurchaseService;

    public MemberPurchaseController(
            MemberPurchaseService memberPurchaseService
    ) {
        this.memberPurchaseService = memberPurchaseService;
    }

    @GetMapping
    public List<MemberPurchaseResponse> listPurchases(
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return memberPurchaseService.listPurchases(userId);
    }

    @GetMapping("/{purchaseId}")
    public MemberPurchaseResponse getPurchase(
            @PathVariable UUID purchaseId,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();

        return memberPurchaseService.getPurchase(userId, purchaseId);
    }
}