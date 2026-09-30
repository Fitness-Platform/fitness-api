package com.fitnessplatform.access;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/program-accesses")
public class ProgramAccessController {

    private final ProgramAccessService programAccessService;

    public ProgramAccessController(
            ProgramAccessService programAccessService
    ) {
        this.programAccessService = programAccessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramAccessResponse grant(
            @Valid
            @RequestBody
            ProgramAccessRequest request
    ) {
        return programAccessService
                .grant(request);
    }

    @GetMapping("/{accessId}")
    public ProgramAccessResponse findById(
            @PathVariable
            UUID accessId
    ) {
        return programAccessService
                .findById(accessId);
    }

    @GetMapping
    public List<ProgramAccessResponse> findAll(
            @RequestParam(required = false)
            UUID userId,

            @RequestParam(required = false)
            UUID programId
    ) {
        return programAccessService
                .findAll(
                        userId,
                        programId
                );
    }

    @PostMapping("/{accessId}/revoke")
    public ProgramAccessResponse revoke(
            @PathVariable
            UUID accessId
    ) {
        return programAccessService.revoke(
                accessId
        );
    }

}
