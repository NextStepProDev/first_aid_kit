package com.firstaidkit.controller.dto.error;

public record FieldValidationError(String field, Object rejectedValue, String message) {}
