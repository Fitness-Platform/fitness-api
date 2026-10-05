package com.fitnessplatform.access;

import com.fitnessplatform.workout.WorkoutExercise;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "program_access_exercise_loads",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_program_access_exercise_load",
                        columnNames = {
                                "program_access_id",
                                "workout_exercise_id"
                        }
                )
        }
)
public class ProgramAccessExerciseLoad {

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
            name = "workout_exercise_id",
            nullable = false
    )
    private WorkoutExercise workoutExercise;

    @Column(
            name = "weight_lb",
            nullable = false,
            precision = 8,
            scale = 2
    )
    private BigDecimal weightLb;

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

    protected ProgramAccessExerciseLoad() {
    }

    public ProgramAccessExerciseLoad(
            ProgramAccess programAccess,
            WorkoutExercise workoutExercise,
            BigDecimal weightLb
    ) {
        this.programAccess =
                programAccess;

        this.workoutExercise =
                workoutExercise;

        this.weightLb =
                weightLb;
    }

    public void updateWeight(
            BigDecimal weightLb
    ) {
        this.weightLb =
                weightLb;
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

    public WorkoutExercise getWorkoutExercise() {
        return workoutExercise;
    }

    public BigDecimal getWeightLb() {
        return weightLb;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}