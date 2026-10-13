package com.fitnessplatform.access;

import com.fitnessplatform.purchase.Purchase;
import com.fitnessplatform.purchase.PurchaseStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
public class PurchasedProgramAccessService {

    private final ProgramAccessRepository programAccessRepository;

    public PurchasedProgramAccessService(
            ProgramAccessRepository programAccessRepository
    ) {
        this.programAccessRepository = programAccessRepository;
    }

    @Transactional
    public void grantForPurchase(Purchase purchase) {

        Objects.requireNonNull(
                purchase,
                "Purchase is required"
        );

        if (purchase.getStatus() != PurchaseStatus.PAID) {
            throw new IllegalStateException(
                    "Only paid purchases can grant program access"
            );
        }

        Instant paidAt = purchase.getPaidAt();

        if (paidAt == null) {
            throw new IllegalStateException(
                    "Paid purchase must have a payment timestamp"
            );
        }

        if (programAccessRepository.findByPurchaseId(
                purchase.getId()
        ).isPresent()) {
            return;
        }

        Instant expiresAt = paidAt
                .atZone(ZoneOffset.UTC)
                .plusMonths(6)
                .toInstant();

        ProgramAccess access = new ProgramAccess(
                purchase,
                paidAt,
                expiresAt
        );

        programAccessRepository.saveAndFlush(access);
    }
}