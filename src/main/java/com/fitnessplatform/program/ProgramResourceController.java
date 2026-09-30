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
@RequestMapping("/api/admin/programs/{programId}/resources")
public class ProgramResourceController {

    private final ProgramResourceService programResourceService;

    public ProgramResourceController(
            ProgramResourceService programResourceService
    ) {
        this.programResourceService =
                programResourceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramResourceResponse create(
            @PathVariable UUID programId,
            @Valid @RequestBody ProgramResourceRequest request
    ) {
        return ProgramResourceResponse.from(
                programResourceService.create(
                        programId,
                        request
                )
        );
    }

    @GetMapping
    public List<ProgramResourceResponse> findAll(
            @PathVariable UUID programId
    ) {
        return programResourceService
                .findAll(programId)
                .stream()
                .map(ProgramResourceResponse::from)
                .toList();
    }

    @GetMapping("/{programResourceId}")
    public ProgramResourceResponse findById(
            @PathVariable UUID programId,
            @PathVariable UUID programResourceId
    ) {
        return ProgramResourceResponse.from(
                programResourceService.findById(
                        programId,
                        programResourceId
                )
        );
    }

    @PutMapping("/{programResourceId}")
    public ProgramResourceResponse update(
            @PathVariable UUID programId,
            @PathVariable UUID programResourceId,
            @Valid @RequestBody ProgramResourceRequest request
    ) {
        return ProgramResourceResponse.from(
                programResourceService.update(
                        programId,
                        programResourceId,
                        request
                )
        );
    }

    @DeleteMapping("/{programResourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID programId,
            @PathVariable UUID programResourceId
    ) {
        programResourceService.delete(
                programId,
                programResourceId
        );
    }
}