package com.fitnessplatform.exercise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExerciseRepository extends JpaRepository <Exercise, UUID> {
}
