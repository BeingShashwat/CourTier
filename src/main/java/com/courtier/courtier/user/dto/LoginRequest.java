package com.courtier.courtier.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Required: email")
        @Email(message = "Invalid mail format")
        String email,

        @NotBlank(message = "Required: password")
        String password
) {
}
