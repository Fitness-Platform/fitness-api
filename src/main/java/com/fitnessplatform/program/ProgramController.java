package com.fitnessplatform.program;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(
            ProgramService programService
    ) {
        this.programService = programService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramResponse create(
            @Valid @RequestBody ProgramRequest request
    ) {
        Program program = programService.create(request);

        return ProgramResponse.from(program);
    }

    @GetMapping
    public List<ProgramResponse> findAll() {
        return programService
                .findAll()
                .stream()
                .map(ProgramResponse::from)
                .toList();
    }

    @GetMapping("/{programId}")
    public ProgramResponse findById(
            @PathVariable UUID programId
    ) {
        return ProgramResponse.from(
                programService.findById(programId)
        );
    }

    @PutMapping("/{programId}")
    public ProgramResponse update(
            @PathVariable UUID programId,
            @Valid @RequestBody ProgramRequest request
    ) {
        Program program =
                programService.update(
                        programId,
                        request
                );

        return ProgramResponse.from(program);
    }

    @PostMapping("/{programId}/publish")
    public ProgramResponse publish(
            @PathVariable UUID programId
    ) {
        return ProgramResponse.from(
                programService.publish(programId)
        );
    }

    @PostMapping("/{programId}/draft")
    public ProgramResponse moveToDraft(
            @PathVariable UUID programId
    ) {
        return ProgramResponse.from(
                programService.moveToDraft(programId)
        );
    }

    @DeleteMapping("/{programId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID programId
    ) {
        programService.delete(programId);
    }
}
