package com.courtier.courtier.user.dto;

public record CachedUser(
        Long id,
        String email,
        String password,
        String fullName,
        String role,
        boolean enabled
) {
}