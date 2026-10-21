package com.fitnessplatform.access;

import com.fitnessplatform.program.ProgramWeekWorkout;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;

@Service
public class ProgramAccessWorkoutHistoryService {

    private final ProgramAccessRepository
            programAccessRepository;

    private final ProgramAccessWorkoutRepository
            programAccessWorkoutRepository;

    private final ProgramAccessWorkoutSetRepository
            programAccessWorkoutSetRepository;

    public ProgramAccessWorkoutHistoryService(
            ProgramAccessRepository programAccessRepository,
            ProgramAccessWorkoutRepository programAccessWorkoutRepository,
            ProgramAccessWorkoutSetRepository programAccessWorkoutSetRepository
    ) {
        this.programAccessRepository =
                programAccessRepository;

        this.programAccessWorkoutRepository =
                programAccessWorkoutRepository;

        this.programAccessWorkoutSetRepository =
                programAccessWorkoutSetRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ProgramAccessWorkout>
    findPreviousCompletedWorkout(
            ProgramAccess currentAccess,
            ProgramWeekWorkout programWeekWorkout
    ) {
        List<ProgramAccess> previousAccesses =
                programAccessRepository
                        .findPreviousAccesses(
                                currentAccess
                                        .getUser()
                                        .getId(),
                                currentAccess
                                        .getProgram()
                                        .getId(),
                                currentAccess
                                        .getId(),
                                currentAccess
                                        .getStartsAt(),
                                currentAccess
                                        .getCreatedAt()
                        );

        for (
                ProgramAccess previousAccess
                : previousAccesses
        ) {
            Optional<ProgramAccessWorkout>
                    previousWorkout =
                    programAccessWorkoutRepository
                            .findByProgramAccessIdAndProgramWeekWorkoutIdAndCompletedAtIsNotNull(
                                    previousAccess.getId(),
                                    programWeekWorkout.getId()
                            );

            if (previousWorkout.isPresent()) {
                return previousWorkout;
            }
        }

        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<ProgramAccessWorkoutSet>>
    findPreviousPerformance(
            ProgramAccess currentAccess,
            ProgramWeekWorkout programWeekWorkout
    ) {
        Optional<ProgramAccessWorkout>
                previousWorkout =
                findPreviousCompletedWorkout(
                        currentAccess,
                        programWeekWorkout
                );

        if (previousWorkout.isEmpty()) {
            return Map.of();
        }

        List<ProgramAccessWorkoutSet> sets =
                programAccessWorkoutSetRepository
                        .findAllByProgramAccessWorkoutIdOrderByCompletedAtAsc(
                                previousWorkout
                                        .orElseThrow()
                                        .getId()
                        );

        return sets.stream()
                .collect(
                        Collectors.groupingBy(
                                set ->
                                        set
                                                .getWorkoutExercise()
                                                .getId()
                        )
                );
    }
}