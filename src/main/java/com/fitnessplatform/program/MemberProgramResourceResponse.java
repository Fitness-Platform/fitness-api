package com.fitnessplatform.program;

import java.util.UUID;

public record MemberProgramResourceResponse(
        UUID resourceId,
        String title,
        String description,
        String url,
        Integer position
) {
    public static MemberProgramResourceResponse from(
            ProgramResource resource
    ) {
        return new MemberProgramResourceResponse(
                resource.getId(),
                resource.getTitle(),
                resource.getDescription(),
                resource.getUrl(),
                resource.getPosition()
        );
    }
}
