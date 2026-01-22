package com.firstaid.controller.dto.error;

import java.util.List;

public record ValidationErrorMessageDTO(
        int status,
        String message,
        String timestamp,
        List<FieldValidationError> errors
    ) {}
