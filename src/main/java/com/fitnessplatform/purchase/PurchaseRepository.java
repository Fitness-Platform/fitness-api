package com.fitnessplatform.purchase;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query("""
            SELECT p
            FROM Purchase p
            JOIN FETCH p.program
            WHERE p.user.id = :userId
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<Purchase> findAllByUserIdForMember(
            @Param("userId") UUID userId
    );

    @Query("""
            SELECT p
            FROM Purchase p
            JOIN FETCH p.program
            WHERE p.id = :purchaseId
              AND p.user.id = :userId
            """)
    Optional<Purchase> findByIdAndUserIdForMember(
            @Param("purchaseId") UUID purchaseId,
            @Param("userId") UUID userId
    );

    @Query("""
        SELECT p
        FROM Purchase p
        JOIN FETCH p.user
        JOIN FETCH p.program
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    List<Purchase> findAllForAdmin();

    @Query("""
        SELECT p
        FROM Purchase p
        JOIN FETCH p.user
        JOIN FETCH p.program
        WHERE p.id = :purchaseId
        """)
    Optional<Purchase> findByIdForAdmin(
            @Param("purchaseId") UUID purchaseId
    );
}