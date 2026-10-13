package com.fitnessplatform.purchase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminPurchaseService {

    private final PurchaseRepository purchaseRepository;

    public AdminPurchaseService(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminPurchaseResponse> listPurchases() {
        return purchaseRepository.findAllForAdmin()
                .stream()
                .map(AdminPurchaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPurchaseResponse getPurchase(UUID purchaseId) {
        Purchase purchase = purchaseRepository
                .findByIdForAdmin(purchaseId)
                .orElseThrow(AdminPurchaseNotFoundException::new);

        return AdminPurchaseResponse.from(purchase);
    }
}