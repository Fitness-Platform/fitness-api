package com.fitnessplatform.access;

import com.fitnessplatform.program.ProgramWeekWorkout;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "program_access_workouts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_program_access_workout",
                        columnNames = {
                                "program_access_id",
                                "program_week_workout_id"
                        }
                )
        }
)
public class ProgramAccessWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "program_access_id",
            nullable = false
    )
    private ProgramAccess programAccess;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "program_week_workout_id",
            nullable = false
    )
    private ProgramWeekWorkout programWeekWorkout;

    @Column(
            name = "started_at",
            nullable = false
    )
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

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

    protected ProgramAccessWorkout() {
    }

    public ProgramAccessWorkout(
            ProgramAccess programAccess,
            ProgramWeekWorkout programWeekWorkout,
            Instant startedAt
    ) {
        this.programAccess =
                programAccess;

        this.programWeekWorkout =
                programWeekWorkout;

        this.startedAt =
                startedAt;
    }

    public void complete(
            Instant completedAt
    ) {
        this.completedAt =
                completedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now =
                Instant.now();

        createdAt =
                now;

        updatedAt =
                now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt =
                Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ProgramAccess getProgramAccess() {
        return programAccess;
    }

    public ProgramWeekWorkout getProgramWeekWorkout() {
        return programWeekWorkout;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}