package com.fitnessplatform.program;

import com.fitnessplatform.workout.Workout;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "program_week_workouts")
public class ProgramWeekWorkout {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "program_week_id",
            nullable = false
    )
    private ProgramWeek programWeek;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "workout_id",
            nullable = false
    )
    private Workout workout;

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

    protected ProgramWeekWorkout() {
    }

    public ProgramWeekWorkout(
            ProgramWeek programWeek,
            Workout workout,
            Integer position
    ) {
        this.programWeek = programWeek;
        this.workout = workout;
        this.position = position;
    }

    public void updatePosition(
            Integer position
    ) {
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

    public ProgramWeek getProgramWeek() {
        return programWeek;
    }

    public Workout getWorkout() {
        return workout;
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
