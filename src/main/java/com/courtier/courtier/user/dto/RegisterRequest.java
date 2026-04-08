package com.courtier.courtier.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Required: Full Name")
        String fullName,

        @NotBlank(message = "Required: Email")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Required: Password")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number")
        String phone
) {
}
