package com.courtier.courtier.user.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String fullName
) {
}
