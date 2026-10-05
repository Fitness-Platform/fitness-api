package com.fitnessplatform.program;

import com.fitnessplatform.access.*;
import com.fitnessplatform.workout.WorkoutExercise;
import com.fitnessplatform.workout.WorkoutExerciseNotFoundException;
import com.fitnessplatform.workout.WorkoutExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MemberProgramService {

    private final ProgramAccessAuthorizationService
            programAccessAuthorizationService;

    private final ProgramWeekRepository
            programWeekRepository;

    private final ProgramResourceRepository
            programResourceRepository;

    private final ProgramWeekWorkoutRepository
            programWeekWorkoutRepository;

    private final WorkoutExerciseRepository
            workoutExerciseRepository;

    private final ProgramWeekUnlockService
            programWeekUnlockService;

    private final ProgramAccessExerciseLoadService
            programAccessExerciseLoadService;

    public MemberProgramService(
            ProgramAccessAuthorizationService
                    programAccessAuthorizationService,
            ProgramWeekRepository
                    programWeekRepository,
            ProgramResourceRepository
                    programResourceRepository,
            ProgramWeekWorkoutRepository
                    programWeekWorkoutRepository,
            WorkoutExerciseRepository
                    workoutExerciseRepository,
            ProgramWeekUnlockService
                    programWeekUnlockService,

            ProgramAccessExerciseLoadService
                    programAccessExerciseLoadService
    ) {
        this.programAccessAuthorizationService =
                programAccessAuthorizationService;

        this.programWeekRepository =
                programWeekRepository;

        this.programResourceRepository =
                programResourceRepository;

        this.programWeekWorkoutRepository =
                programWeekWorkoutRepository;

        this.workoutExerciseRepository =
                workoutExerciseRepository;

        this.programWeekUnlockService =
                programWeekUnlockService;

        this.programAccessExerciseLoadService =
                programAccessExerciseLoadService;
    }

    @Transactional(readOnly = true)
    public List<MemberProgramSummaryResponse> findMyPrograms(
            UUID userId
    ) {
        return programAccessAuthorizationService
                .findActiveAccesses(userId)
                .stream()
                .filter(
                        access ->
                                access.getProgram().getStatus()
                                        == ProgramStatus.PUBLISHED
                )
                .map(
                        MemberProgramSummaryResponse::from
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberProgramDetailResponse findMyProgram(
            UUID userId,
            UUID programId
    ) {
        ProgramAccess access =
                requirePublishedProgramAccess(
                        userId,
                        programId
                );

        List<ProgramWeek> weeks =
                programWeekRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                programId
                        );

        List<ProgramResource> resources =
                programResourceRepository
                        .findAllByProgramIdOrderByPositionAsc(
                                programId
                        );

        List<MemberProgramWeekResponse> weekResponses =
                weeks.stream()
                        .map(
                                week -> {
                                    Instant unlocksAt =
                                            programWeekUnlockService
                                                    .calculateUnlocksAt(
                                                            access,
                                                            week
                                                    );

                                    boolean unlocked =
                                            programWeekUnlockService
                                                    .isUnlocked(
                                                            access,
                                                            week
                                                    );

                                    return MemberProgramWeekResponse
                                            .from(
                                                    week,
                                                    unlocked,
                                                    unlocksAt
                                            );
                                }
                        )
                        .toList();

        return MemberProgramDetailResponse.from(
                access,
                resources,
                weekResponses
        );
    }

    @Transactional(readOnly = true)
    public MemberProgramWeekDetailResponse findMyProgramWeek(
            UUID userId,
            UUID programId,
            UUID programWeekId
    ) {
        ProgramAccess access =
                requirePublishedProgramAccess(
                        userId,
                        programId
                );

        ProgramWeek week =
                programWeekRepository
                        .findByIdAndProgramId(
                                programWeekId,
                                programId
                        )
                        .orElseThrow(
                                () ->
                                        new ProgramWeekNotFoundException(
                                                programWeekId
                                        )
                        );

        programWeekUnlockService
                .requireUnlocked(
                        access,
                        week
                );

        List<ProgramWeekWorkout> workouts =
                programWeekWorkoutRepository
                        .findAllByProgramWeekIdOrderByPositionAsc(
                                programWeekId
                        );

        return MemberProgramWeekDetailResponse.from(
                week,
                workouts
        );
    }

    @Transactional(readOnly = true)
    public MemberProgramWorkoutDetailResponse findMyProgramWorkout(
            UUID userId,
            UUID programId,
            UUID programWeekId,
            UUID programWeekWorkoutId
    ) {
        ProgramAccess access =
                requirePublishedProgramAccess(
                        userId,
                        programId
                );

        ProgramWeek week =
                programWeekRepository
                        .findByIdAndProgramId(
                                programWeekId,
                                programId
                        )
                        .orElseThrow(
                                () ->
                                        new ProgramWeekNotFoundException(
                                                programWeekId
                                        )
                        );

        programWeekUnlockService
                .requireUnlocked(
                        access,
                        week
                );

        ProgramWeekWorkout association =
                programWeekWorkoutRepository
                        .findByIdAndProgramWeekId(
                                programWeekWorkoutId,
                                programWeekId
                        )
                        .orElseThrow(
                                () ->
                                        new ProgramWeekWorkoutNotFoundException(
                                                programWeekWorkoutId
                                        )
                        );

        List<WorkoutExercise> exercises =
                workoutExerciseRepository
                        .findAllByWorkoutIdOrderByPositionAsc(
                                association
                                        .getWorkout()
                                        .getId()
                        );

        Map<UUID, BigDecimal> currentWeights =
                programAccessExerciseLoadService
                        .resolveCurrentWeights(
                                access,
                                exercises
                        );

        return MemberProgramWorkoutDetailResponse.from(
                association,
                exercises,
                currentWeights
        );
    }

    @Transactional
    public MemberExerciseLoadResponse updateMyExerciseLoad(
            UUID userId,
            UUID programId,
            UUID programWeekId,
            UUID programWeekWorkoutId,
            UUID workoutExerciseId,
            BigDecimal weightLb
    ) {
        ProgramAccess access =
                requirePublishedProgramAccess(
                        userId,
                        programId
                );

        ProgramWeek week =
                programWeekRepository
                        .findByIdAndProgramId(
                                programWeekId,
                                programId
                        )
                        .orElseThrow(
                                () ->
                                        new ProgramWeekNotFoundException(
                                                programWeekId
                                        )
                        );

        programWeekUnlockService
                .requireUnlocked(
                        access,
                        week
                );

        ProgramWeekWorkout association =
                programWeekWorkoutRepository
                        .findByIdAndProgramWeekId(
                                programWeekWorkoutId,
                                programWeekId
                        )
                        .orElseThrow(
                                () ->
                                        new ProgramWeekWorkoutNotFoundException(
                                                programWeekWorkoutId
                                        )
                        );

        WorkoutExercise workoutExercise =
                workoutExerciseRepository
                        .findByIdAndWorkoutId(
                                workoutExerciseId,
                                association
                                        .getWorkout()
                                        .getId()
                        )
                        .orElseThrow(
                                () ->
                                        new WorkoutExerciseNotFoundException(
                                                workoutExerciseId
                                        )
                        );

        ProgramAccessExerciseLoad load =
                programAccessExerciseLoadService
                        .setWeight(
                                access,
                                workoutExercise,
                                weightLb
                        );

        return MemberExerciseLoadResponse.from(
                workoutExercise,
                load.getWeightLb()
        );
    }

    private ProgramAccess requirePublishedProgramAccess(
            UUID userId,
            UUID programId
    ) {
        ProgramAccess access =
                programAccessAuthorizationService
                        .requireActiveAccess(
                                userId,
                                programId
                        );

        if (
                access.getProgram().getStatus()
                        != ProgramStatus.PUBLISHED
        ) {
            throw new ProgramAccessDeniedException(
                    programId
            );
        }

        return access;
    }
}