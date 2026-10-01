package com.fitnessplatform.program;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/programs")
public class MemberProgramController {

    private final MemberProgramService memberProgramService;

    public MemberProgramController(
            MemberProgramService
                memberProgramService
    ) {
        this.memberProgramService = memberProgramService;
    }

    @GetMapping
    public List<MemberProgramSummaryResponse> findMyPrograms(
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return memberProgramService
                .findMyPrograms(userId);
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

    @GetMapping("/{programId}/weeks/{programWeekId}")
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

    @GetMapping("/{programId}/weeks/{programWeekId}/workouts/{programWeekWorkoutId}")
    public MemberProgramWorkoutDetailResponse findMyProgramWorkout(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @PathVariable UUID programWeekWorkoutId,
            Authentication authentication
    ) {
        UUID userId =
                (UUID) authentication.getPrincipal();

        return  memberProgramService
                .findMyProgramWorkout(
                        userId,
                        programId,
                        programWeekId,
                        programWeekWorkoutId
                );
    }
}
