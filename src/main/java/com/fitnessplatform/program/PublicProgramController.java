package com.fitnessplatform.program;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programs")
public class PublicProgramController {

    private final ProgramService programService;

    public PublicProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping
    public List<PublicProgramResponse> findAll() {
        return programService.findPublished()
                .stream()
                .map(PublicProgramResponse::from)
                .toList();
    }

    @GetMapping("/{programId}")
    public PublicProgramResponse findById(@PathVariable UUID programId) {
        return PublicProgramResponse.from(
                programService.findPublishedById(programId)
        );
    }
}
