package com.fitnessplatform.purchase;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository
        extends JpaRepository<Purchase, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Purchase p
            WHERE p.id = :purchaseId
            """)
    Optional<Purchase> findByIdForUpdate(
            @Param("purchaseId") UUID purchaseId
    );
}