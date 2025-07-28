package com.drugs.controller.exception;

import java.util.List;

public record ValidationErrorMessageDTO(
        int status,
        String message,
        String timestamp,
        List<FieldValidationError> errors
    ) {}