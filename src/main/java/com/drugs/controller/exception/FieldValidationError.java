package com.drugs.controller.exception;

public record FieldValidationError(String field, Object rejectedValue, String message) {}