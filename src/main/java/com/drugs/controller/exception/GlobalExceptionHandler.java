package com.drugs.controller.exception;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record FieldValidationError(String field, Object rejectedValue, String message) {
    }

    @PostConstruct
    public void init() {
        System.out.println(">>> RestExceptionHandler initialized!");
        // This method can be used for any initialization logic if needed
        // For example, you could log that the handler is ready to process exceptions
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<FieldValidationError>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(
                        FieldError::getField,
                        err -> new FieldValidationError(
                                err.getField(),
                                err.getRejectedValue(),
                                err.getDefaultMessage()
                        ),
                        (first, second) -> first // keep only the first error per field
                ))
                .values()
                .stream()
                .toList();

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<List<FieldValidationError>> handleConstraintViolation(ConstraintViolationException ex) {
        System.out.println(">>> Using constraint violation handler!");
        List<FieldValidationError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldValidationError(
                        extractFieldName(violation.getPropertyPath().toString()),
                        violation.getInvalidValue(),
                        violation.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(errors);
    }

    private String extractFieldName(String path) {
        int lastDot = path.lastIndexOf(".");
        return lastDot != -1 ? path.substring(lastDot + 1) : path;
    }

    @ExceptionHandler(DrugNotFoundException.class)
    public ResponseEntity<String> handleDrugNotFound(DrugNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        System.err.println("Error: " + ex.getMessage());
        return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBindException(BindException ex) {
        FieldError error = ex.getFieldError();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "Invalid input");
        response.put("field", error != null ? error.getField() : null);
        response.put("rejectedValue", error != null ? error.getRejectedValue() : null);
        response.put("message", error != null ? error.getDefaultMessage() : "Invalid value");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("timestamp", OffsetDateTime.now().toString());

        return response;
    }
}