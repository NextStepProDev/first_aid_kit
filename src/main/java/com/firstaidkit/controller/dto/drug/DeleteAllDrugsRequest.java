package com.firstaidkit.controller.dto.drug;

import jakarta.validation.constraints.NotBlank;

public record DeleteAllDrugsRequest(
        @NotBlank(message = "Password is required")
        String password
) {
}
