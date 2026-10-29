package com.fitnessplatform.exercise;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository <Exercise, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Exercise e where e.id = :id")
    Optional<Exercise> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
}
