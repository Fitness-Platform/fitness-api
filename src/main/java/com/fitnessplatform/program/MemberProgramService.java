package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccess;
import com.fitnessplatform.access.ProgramAccessAuthorizationService;
import com.fitnessplatform.access.ProgramAccessDeniedException;
import com.fitnessplatform.workout.WorkoutExercise;
import com.fitnessplatform.workout.WorkoutExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
                    programWeekUnlockService
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

        return MemberProgramWorkoutDetailResponse.from(
                association,
                exercises
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