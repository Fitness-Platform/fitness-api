package com.fitnessplatform.program;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/admin/programs/{programId}/weeks")
public class ProgramWeekController {

    private final ProgramWeekService programWeekService;

    public ProgramWeekController(
            ProgramWeekService programWeekService
    ) {
        this.programWeekService = programWeekService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramWeekResponse create(
            @PathVariable UUID programId,
            @Valid @RequestBody
            ProgramWeekRequest request
    ) {
        return ProgramWeekResponse.from(
                programWeekService.create(
                        programId,
                        request
                )
        );
    }

    @GetMapping
    public List<ProgramWeekResponse> findAll(
            @PathVariable UUID programId
    ) {
        return programWeekService
                .findAll(programId)
                .stream()
                .map(ProgramWeekResponse::from)
                .toList();

    }

    @GetMapping("/{programWeekId}")
    public ProgramWeekResponse findById(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId
    ) {
        return ProgramWeekResponse.from(
                programWeekService.findById(
                        programId,
                        programWeekId
                )
        );
    }

    @PutMapping("/{programWeekId}")
    public ProgramWeekResponse update(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId,
            @Valid @RequestBody
            ProgramWeekRequest request
    ) {
        return ProgramWeekResponse.from(
                programWeekService.update(
                        programId,
                        programWeekId,
                        request
                )
        );
    }

    @DeleteMapping("/{programWeekId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID programId,
            @PathVariable UUID programWeekId
    ) {
        programWeekService.delete(
                programId,
                programWeekId
        );
    }
}
