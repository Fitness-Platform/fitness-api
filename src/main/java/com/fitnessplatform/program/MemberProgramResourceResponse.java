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
        return from(resource, resource.getUrl());
    }
    public static MemberProgramResourceResponse from(ProgramResource resource, String url) {
        return new MemberProgramResourceResponse(
                resource.getId(),
                resource.getTitle(),
                resource.getDescription(),
                url,
                resource.getPosition()
        );
    }
    @Override
    public String toString() { return "MemberProgramResourceResponse[media URL redacted]"; }
}
