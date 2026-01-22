package com.firstaid.controller.dto.error;

public record FieldValidationError(String field, Object rejectedValue, String message) {}
