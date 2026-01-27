package com.firstaidkit.controller.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastEmailRequest(
        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject cannot exceed 200 characters")
        String subject,

        @NotBlank(message = "Message is required")
        @Size(max = 10000, message = "Message cannot exceed 10000 characters")
        String message
) {}
