package com.fitnessplatform.exercise;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(length = 120)
    private String equipment;

    @Column(
            name = "video_url",
            length = 2048
    )
    private String videoUrl;

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

    protected Exercise() {
    }

    public Exercise(
            String name,
            String instructions,
            String equipment,
            String videoUrl
    ) {
        this.name = name;
        this.instructions = instructions;
        this.equipment = equipment;
        this.videoUrl = videoUrl;
    }

    public void update(
            String name,
            String instructions,
            String equipment,
            String videoUrl
    ) {
        this.name = name;
        this.instructions = instructions;
        this.equipment = equipment;
        this.videoUrl = videoUrl;
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

    public String getInstructions() {
        return instructions;
    }

    public String getEquipment() {
        return equipment;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
