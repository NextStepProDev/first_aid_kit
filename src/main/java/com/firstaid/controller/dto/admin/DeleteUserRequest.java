package com.firstaid.controller.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteUserRequest(
        @NotBlank(message = "Password is required for confirmation")
        @Size(min = 1, max = 256, message = "Password must be between 1 and 256 characters")
        String password
) {}
