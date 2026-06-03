package com.courtier.courtier.case_.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddCaseRequest(

        @NotBlank(message = "CNR number is required")
        @Pattern(
                regexp = "^[A-Z]{4}[0-9]{12}$",
                message = "Invalid CNR format"
        )
        String cnrNumber

) {}