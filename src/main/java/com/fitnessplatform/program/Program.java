package com.fitnessplatform.program;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "programs")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private ProgramStatus status;

    @Column(name = "price_cents")
    private Long priceCents;

    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

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

    protected Program() {
    }

    public Program(
            String name,
            String description
    ) {
        this.name = name;
        this.description = description;
        this.status = ProgramStatus.DRAFT;
        this.currency = "USD";
    }

    public void update(
            String name,
            String description
    ) {
        this.name = name;
        this.description = description;
    }

    public void publish() {
        this.status = ProgramStatus.PUBLISHED;
    }

    public void moveToDraft() {
        this.status = ProgramStatus.DRAFT;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProgramStatus getStatus() {
        return status;
    }

    public Long getPriceCents() {
        return priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updatePrice(
            Long priceCents
    ) {
        this.priceCents = priceCents;
    }
}
