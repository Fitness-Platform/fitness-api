package com.fitnessplatform.auth;

import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRole;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        UserRole role
) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }
}