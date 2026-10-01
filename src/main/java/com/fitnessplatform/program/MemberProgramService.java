package com.fitnessplatform.program;

import com.fitnessplatform.access.ProgramAccess;
import com.fitnessplatform.access.ProgramAccessAuthorizationService;
import com.fitnessplatform.workout.WorkoutExercise;
import com.fitnessplatform.workout.WorkoutExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                    workoutExerciseRepository
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
    }

    @Transactional(readOnly = true)
    public List<MemberProgramSummaryResponse> findMyPrograms(
            UUID userId
    ) {
        return programAccessAuthorizationService
                .findActiveAccesses(userId)
                .stream()
                .map(
                        access ->
                                MemberProgramSummaryResponse
                                        .from(access)
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberProgramDetailResponse findMyProgram(
            UUID userId,
            UUID programId
    ) {
        ProgramAccess access =
                programAccessAuthorizationService
                        .requireActiveAccess(
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

        return MemberProgramDetailResponse.from(
                access,
                resources,
                weeks
        );
    }

    @Transactional(readOnly = true)
    public MemberProgramWeekDetailResponse findMyProgramWeek(
            UUID userId,
            UUID programId,
            UUID programWeekId
    ) {
        programAccessAuthorizationService
                .requireActiveAccess(
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
        programAccessAuthorizationService
                .requireActiveAccess(
                        userId,
                        programId
                );

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


}
