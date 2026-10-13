package com.fitnessplatform.purchase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MemberPurchaseService {

    private final PurchaseRepository purchaseRepository;

    public MemberPurchaseService(
            PurchaseRepository purchaseRepository
    ) {
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberPurchaseResponse> listPurchases(UUID userId) {
        return purchaseRepository
                .findAllByUserIdForMember(userId)
                .stream()
                .map(MemberPurchaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberPurchaseResponse getPurchase(
            UUID userId,
            UUID purchaseId
    ) {
        Purchase purchase = purchaseRepository
                .findByIdAndUserIdForMember(purchaseId, userId)
                .orElseThrow(
                        () -> new MemberPurchaseNotFoundException()
                );

        return MemberPurchaseResponse.from(purchase);
    }
}