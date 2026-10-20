package com.fitnessplatform.access;

import com.fitnessplatform.workout.WorkoutExercise;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "program_access_workout_sets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_program_access_workout_set",
                        columnNames = {
                                "program_access_workout_id",
                                "workout_exercise_id",
                                "set_number"
                        }
                )
        }
)
public class ProgramAccessWorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "program_access_workout_id",
            nullable = false
    )
    private ProgramAccessWorkout programAccessWorkout;

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
            name = "set_number",
            nullable = false
    )
    private Integer setNumber;

    @Column(
            name = "reps",
            nullable = false
    )
    private Integer reps;

    @Column(
            name = "weight_lb",
            nullable = false,
            precision = 8,
            scale = 2
    )
    private BigDecimal weightLb;

    @Column(
            name = "completed_at",
            nullable = false
    )
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

    protected ProgramAccessWorkoutSet() {
    }

    public ProgramAccessWorkoutSet(
            ProgramAccessWorkout programAccessWorkout,
            WorkoutExercise workoutExercise,
            Integer setNumber,
            Integer reps,
            BigDecimal weightLb,
            Instant completedAt
    ) {
        this.programAccessWorkout =
                programAccessWorkout;

        this.workoutExercise =
                workoutExercise;

        this.setNumber =
                setNumber;

        this.reps =
                reps;

        this.weightLb =
                weightLb;

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

    public ProgramAccessWorkout getProgramAccessWorkout() {
        return programAccessWorkout;
    }

    public WorkoutExercise getWorkoutExercise() {
        return workoutExercise;
    }

    public Integer getSetNumber() {
        return setNumber;
    }

    public Integer getReps() {
        return reps;
    }

    public BigDecimal getWeightLb() {
        return weightLb;
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

    public void updatePerformance(
            Integer reps,
            BigDecimal weightLb
    ) {
        this.reps =
                reps;

        this.weightLb =
                weightLb;
    }
}