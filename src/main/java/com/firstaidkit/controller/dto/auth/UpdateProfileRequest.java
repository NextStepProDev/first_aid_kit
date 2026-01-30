package com.firstaidkit.controller.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Username is required")
        @Size(min = 5, max = 36, message = "Username must be between 5 and 36 characters")
        String username
) {}
