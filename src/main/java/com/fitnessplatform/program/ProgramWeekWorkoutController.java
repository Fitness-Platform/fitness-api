package com.fitnessplatform.program;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/admin/programs/{programId}/weeks/{programWeekId}/workouts"
)
public class ProgramWeekWorkoutController {

    private final ProgramWeekWorkoutService programWeekWorkoutService;

    public ProgramWeekWorkoutController(
            ProgramWeekWorkoutService programWeekWorkoutService
    ) {
        this.programWeekWorkoutService =
                programWeekWorkoutService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramWeekWorkoutResponse create(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @Valid @RequestBody
            ProgramWeekWorkoutCreateRequest request
    ) {
        return ProgramWeekWorkoutResponse.from(
                programWeekWorkoutService.create(
                        programId,
                        programWeekId,
                        request
                )
        );
    }

    @GetMapping
    public List<ProgramWeekWorkoutResponse> findAll(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId
    ) {
        return programWeekWorkoutService
                .findAll(
                        programId,
                        programWeekId
                )
                .stream()
                .map(ProgramWeekWorkoutResponse::from)
                .toList();
    }

    @GetMapping("/{programWeekWorkoutId}")
    public ProgramWeekWorkoutResponse findById(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @PathVariable UUID programWeekWorkoutId
    ) {
        return ProgramWeekWorkoutResponse.from(
                programWeekWorkoutService.findById(
                        programId,
                        programWeekId,
                        programWeekWorkoutId
                )
        );
    }

    @PutMapping("/{programWeekWorkoutId}")
    public ProgramWeekWorkoutResponse update(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @PathVariable UUID programWeekWorkoutId,
            @Valid @RequestBody
            ProgramWeekWorkoutUpdateRequest request
    ) {
        return ProgramWeekWorkoutResponse.from(
                programWeekWorkoutService.update(
                        programId,
                        programWeekId,
                        programWeekWorkoutId,
                        request
                )
        );
    }

    @DeleteMapping("/{programWeekWorkoutId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @PathVariable UUID programWeekWorkoutId
    ) {
        programWeekWorkoutService.delete(
                programId,
                programWeekId,
                programWeekWorkoutId
        );
    }
}