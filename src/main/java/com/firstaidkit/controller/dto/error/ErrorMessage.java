package com.firstaidkit.controller.dto.error;

import java.time.OffsetDateTime;

public record ErrorMessage(int status, String message, String timestamp) {
    public ErrorMessage(int status, String message) {
        this(status, message, OffsetDateTime.now().toString());
    }
}
