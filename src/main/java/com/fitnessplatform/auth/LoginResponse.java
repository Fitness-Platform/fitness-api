package com.fitnessplatform.auth;

import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRole;

import java.util.UUID;

public record LoginResponse(
        UUID id,
        String email,
        UserRole role
) {

    public static LoginResponse from(User user) {
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }
}
