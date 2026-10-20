package com.fitnessplatform.program;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/programs")
public class MemberProgramController {

    private final MemberProgramService
            memberProgramService;

    public MemberProgramController(
            MemberProgramService memberProgramService
    ) {
        this.memberProgramService =
                memberProgramService;
    }

    @GetMapping
    public List<MemberProgramSummaryResponse> findMyPrograms(
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return memberProgramService
                .findMyPrograms(
                        userId
                );
    }

    @GetMapping("/{programId}")
    public MemberProgramDetailResponse findMyProgram(
            @PathVariable UUID programId,
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return memberProgramService
                .findMyProgram(
                        userId,
                        programId
                );
    }

    @GetMapping(
            "/{programId}/weeks/{programWeekId}"
    )
    public MemberProgramWeekDetailResponse findMyProgramWeek(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return memberProgramService
                .findMyProgramWeek(
                        userId,
                        programId,
                        programWeekId
                );
    }

    @GetMapping(
            "/{programId}/weeks/{programWeekId}/workouts/{programWeekWorkoutId}"
    )
    public MemberProgramWorkoutDetailResponse findMyProgramWorkout(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @PathVariable UUID programWeekWorkoutId,
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return memberProgramService
                .findMyProgramWorkout(
                        userId,
                        programId,
                        programWeekId,
                        programWeekWorkoutId
                );
    }

    @PutMapping(
            "/{programId}/weeks/{programWeekId}"
                    + "/workouts/{programWeekWorkoutId}"
                    + "/exercises/{workoutExerciseId}/load"
    )
    public MemberExerciseLoadResponse updateMyExerciseLoad(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @PathVariable UUID programWeekWorkoutId,
            @PathVariable UUID workoutExerciseId,
            @Valid @RequestBody
            MemberExerciseLoadRequest request,
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return memberProgramService
                .updateMyExerciseLoad(
                        userId,
                        programId,
                        programWeekId,
                        programWeekWorkoutId,
                        workoutExerciseId,
                        request.weightLb()
                );
    }

    @PutMapping(
            "/{programId}/weeks/{programWeekId}"
                    + "/workouts/{programWeekWorkoutId}"
                    + "/exercises/{workoutExerciseId}"
                    + "/sets/{setNumber}"
    )
    public MemberWorkoutSetResponse saveMyWorkoutSet(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @PathVariable UUID programWeekWorkoutId,
            @PathVariable UUID workoutExerciseId,
            @PathVariable Integer setNumber,
            @Valid @RequestBody
            MemberWorkoutSetRequest request,
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return memberProgramService
                .saveMyWorkoutSet(
                        userId,
                        programId,
                        programWeekId,
                        programWeekWorkoutId,
                        workoutExerciseId,
                        setNumber,
                        request.reps(),
                        request.weightLb()
                );
    }

    @PostMapping(
            "/{programId}/weeks/{programWeekId}"
                    + "/workouts/{programWeekWorkoutId}"
                    + "/complete"
    )
    public MemberWorkoutCompletionResponse completeMyWorkout(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @PathVariable UUID programWeekWorkoutId,
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return memberProgramService
                .completeMyWorkout(
                        userId,
                        programId,
                        programWeekId,
                        programWeekWorkoutId
                );
    }
}