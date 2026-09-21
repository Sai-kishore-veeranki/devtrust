package com.vsk.devtrust.auth;

import java.time.Instant;

public record UserProfileResponse(
        String username,
        String email,
        String fullName,
        Role role,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
