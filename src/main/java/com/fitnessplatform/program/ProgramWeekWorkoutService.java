package com.fitnessplatform.program;

import com.fitnessplatform.workout.Workout;
import com.fitnessplatform.workout.WorkoutNotFoundException;
import com.fitnessplatform.workout.WorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProgramWeekWorkoutService {

    private final ProgramWeekWorkoutRepository programWeekWorkoutRepository;
    private final ProgramRepository programRepository;
    private final ProgramWeekRepository programWeekRepository;
    private final WorkoutRepository workoutRepository;

    public ProgramWeekWorkoutService(
            ProgramWeekWorkoutRepository programWeekWorkoutRepository,
            ProgramRepository programRepository,
            ProgramWeekRepository programWeekRepository,
            WorkoutRepository workoutRepository
    ) {
        this.programWeekWorkoutRepository = programWeekWorkoutRepository;
        this.programRepository = programRepository;
        this.programWeekRepository = programWeekRepository;
        this.workoutRepository = workoutRepository;
    }

    @Transactional
    public ProgramWeekWorkout create(
            UUID programId,
            UUID programWeekId,
            ProgramWeekWorkoutCreateRequest request
    ) {
        findProgram(programId);

        ProgramWeek programWeek =
                findProgramWeek(
                        programId,
                        programWeekId
                );

        Workout workout =
                findWorkout(request.workoutId());

        ensurePositionAvailable(
                programWeekId,
                request.position()
        );

        ProgramWeekWorkout association =
                new ProgramWeekWorkout(
                        programWeek,
                        workout,
                        request.position()
                );

        return programWeekWorkoutRepository.save(
                association
        );
    }

    @Transactional(readOnly = true)
    public List<ProgramWeekWorkout> findAll(
            UUID programId,
            UUID programWeekId
    ) {
        findProgram(programId);

        findProgramWeek(
                programId,
                programWeekId
        );

        return programWeekWorkoutRepository
                .findAllByProgramWeekIdOrderByPositionAsc(
                        programWeekId
                );
    }

    @Transactional(readOnly = true)
    public ProgramWeekWorkout findById(
            UUID programId,
            UUID programWeekId,
            UUID programWeekWorkoutId
    ) {
        findProgram(programId);

        findProgramWeek(
                programId,
                programWeekId
        );

        return findAssociation(
                programWeekId,
                programWeekWorkoutId
        );
    }

    @Transactional
    public ProgramWeekWorkout update(
            UUID programId,
            UUID programWeekId,
            UUID programWeekWorkoutId,
            ProgramWeekWorkoutUpdateRequest request
    ) {
        findProgram(programId);

        findProgramWeek(
                programId,
                programWeekId
        );

        ProgramWeekWorkout association =
                findAssociation(
                        programWeekId,
                        programWeekWorkoutId
                );

        ensurePositionAvailableForUpdate(
                programWeekId,
                request.position(),
                programWeekWorkoutId
        );

        association.updatePosition(
                request.position()
        );

        return association;
    }

    @Transactional
    public void delete(
            UUID programId,
            UUID programWeekId,
            UUID programWeekWorkoutId
    ) {
        findProgram(programId);

        findProgramWeek(
                programId,
                programWeekId
        );

        ProgramWeekWorkout association =
                findAssociation(
                        programWeekId,
                        programWeekWorkoutId
                );

        programWeekWorkoutRepository.delete(
                association
        );
    }

    private Program findProgram(
            UUID programId
    ) {
        return programRepository
                .findById(programId)
                .orElseThrow(
                        () -> new ProgramNotFoundException(
                                programId
                        )
                );
    }

    private ProgramWeek findProgramWeek(
            UUID programId,
            UUID programWeekId
    ) {
        return programWeekRepository
                .findByIdAndProgramId(
                        programWeekId,
                        programId
                )
                .orElseThrow(
                        () -> new ProgramWeekNotFoundException(
                                programWeekId
                        )
                );
    }

    private Workout findWorkout(
            UUID workoutId
    ) {
        return workoutRepository
                .findById(workoutId)
                .orElseThrow(
                        () -> new WorkoutNotFoundException(
                                workoutId
                        )
                );
    }

    private ProgramWeekWorkout findAssociation(
            UUID programWeekId,
            UUID programWeekWorkoutId
    ) {
        return programWeekWorkoutRepository
                .findByIdAndProgramWeekId(
                        programWeekWorkoutId,
                        programWeekId
                )
                .orElseThrow(
                        () -> new ProgramWeekWorkoutNotFoundException(
                                programWeekWorkoutId
                        )
                );
    }

    private void ensurePositionAvailable(
            UUID programWeekId,
            Integer position
    ) {
        boolean positionAlreadyUsed =
                programWeekWorkoutRepository
                        .existsByProgramWeekIdAndPosition(
                                programWeekId,
                                position
                        );

        if (positionAlreadyUsed) {
            throw new ProgramWeekWorkoutPositionConflictException(
                    programWeekId,
                    position
            );
        }
    }

    private void ensurePositionAvailableForUpdate(
            UUID programWeekId,
            Integer position,
            UUID programWeekWorkoutId
    ) {
        boolean positionAlreadyUsed =
                programWeekWorkoutRepository
                        .existsByProgramWeekIdAndPositionAndIdNot(
                                programWeekId,
                                position,
                                programWeekWorkoutId
                        );

        if (positionAlreadyUsed) {
            throw new ProgramWeekWorkoutPositionConflictException(
                    programWeekId,
                    position
            );
        }
    }
}
