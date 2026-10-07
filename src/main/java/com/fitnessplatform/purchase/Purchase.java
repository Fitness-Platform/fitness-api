package com.fitnessplatform.purchase;

import com.fitnessplatform.program.Program;
import com.fitnessplatform.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "program_id",
            nullable = false
    )
    private Program program;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private PurchaseStatus status;

    @Column(
            name = "amount_cents",
            nullable = false
    )
    private Long amountCents;

    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(
            name = "stripe_checkout_session_id",
            unique = true,
            length = 255
    )
    private String stripeCheckoutSessionId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected Purchase() {
    }

    public Purchase(
            User user,
            Program program,
            Long amountCents,
            String currency
    ) {
        this.user = user;
        this.program = program;
        this.amountCents = amountCents;
        this.currency = currency;
        this.status = PurchaseStatus.PENDING;
    }

    public void attachStripeCheckoutSession(
            String stripeCheckoutSessionId
    ) {
        this.stripeCheckoutSessionId =
                stripeCheckoutSessionId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Program getProgram() {
        return program;
    }

    public PurchaseStatus getStatus() {
        return status;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}