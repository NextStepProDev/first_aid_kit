package com.firstaid.controller.exception;

public record FieldValidationError(String field, Object rejectedValue, String message) {}