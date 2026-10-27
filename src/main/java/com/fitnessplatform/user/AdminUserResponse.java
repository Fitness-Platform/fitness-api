package com.fitnessplatform.user;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        UserRole role,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
