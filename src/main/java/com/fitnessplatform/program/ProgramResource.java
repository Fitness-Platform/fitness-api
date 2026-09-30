package com.fitnessplatform.program;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "program_resources")
public class ProgramResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "program_id",
            nullable = false
    )
    private Program program;

    @Column(
            nullable = false,
            length = 150
    )
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(
            nullable = false,
            length = 2048
    )
    private String url;

    @Column(nullable = false)
    private Integer position;

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

    protected ProgramResource() {
    }

    public ProgramResource(
            Program program,
            String title,
            String description,
            String url,
            Integer position
    ) {
        this.program = program;
        this.title = title;
        this.description = description;
        this.url = url;
        this.position = position;
    }

    public void update(
            String title,
            String description,
            String url,
            Integer position
    ) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.position = position;
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

    public Program getProgram() {
        return program;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public Integer getPosition() {
        return position;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}