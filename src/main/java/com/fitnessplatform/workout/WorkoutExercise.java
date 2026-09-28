package com.fitnessplatform.workout;

import com.fitnessplatform.exercise.Exercise;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "workout_exercises")
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "workout_id",
            nullable = false
    )
    private Workout  workout;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "exercise_id",
            nullable = false
    )
    private Exercise exercise;

    @Column(
            nullable = false
    )
    private Integer sets;

    @Column(
            nullable = false,
            length = 50
    )
    private String reps;

    @Column(
            name = "suggested_weight_lb",
            precision = 8,
            scale = 2
    )
    private BigDecimal suggestedWeightLb;

    @Column(
            name = "rest_seconds"
    )
    private Integer restSeconds;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(
            nullable = false
    )
    private Integer position;

    protected WorkoutExercise() {
    }

    public WorkoutExercise(
            Workout workout,
            Exercise exercise,
            Integer sets,
            String reps,
            BigDecimal suggestedWeightLb,
            Integer restSeconds,
            String notes,
            Integer position
    ) {
        this.workout = workout;
        this.exercise = exercise;
        this.sets = sets;
        this.reps = reps;
        this.suggestedWeightLb = suggestedWeightLb;
        this.restSeconds = restSeconds;
        this.notes = notes;
        this.position = position;
    }

    public void update(
            Integer sets,
            String reps,
            BigDecimal suggestedWeightLb,
            Integer restSeconds,
            String notes,
            Integer position
    ) {
        this.sets = sets;
        this.reps = reps;
        this.suggestedWeightLb = suggestedWeightLb;
        this.restSeconds = restSeconds;
        this.notes = notes;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public Workout getWorkout() {
        return workout;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Integer getSets() {
        return sets;
    }

    public String getReps() {
        return reps;
    }

    public BigDecimal getSuggestedWeightLb() {
        return suggestedWeightLb;
    }

    public Integer getRestSeconds() {
        return restSeconds;
    }

    public String getNotes() {
        return notes;
    }

    public Integer getPosition() {
        return position;
    }
}
