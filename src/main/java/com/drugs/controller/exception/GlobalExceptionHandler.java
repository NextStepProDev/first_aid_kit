package com.drugs.controller.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles validation errors for @Valid annotated request bodies.
     * Returns a list of validation error details for each field.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @SuppressWarnings("unused")
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

        log.warn("Validation error occurred: {}", errors);

        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles constraint violations for request parameters (e.g. @RequestParam, @PathVariable).
     * Returns a list of field validation errors.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<List<FieldValidationError>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation handler triggered: {}", ex.getMessage());
        List<FieldValidationError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldValidationError(
                        extractFieldName(violation.getPropertyPath().toString()),
                        violation.getInvalidValue(),
                        violation.getMessage()))
                .toList();
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles DrugNotFoundException when a requested drug does not exist.
     * Returns a 404 NOT FOUND status with an error message.
     */
    @ExceptionHandler(DrugNotFoundException.class)
    public ResponseEntity<String> handleDrugNotFound(DrugNotFoundException ex) {
        log.warn("Drug not found: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles all uncaught exceptions and returns a generic 500 INTERNAL SERVER ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        log.error("Unhandled exception caught: ", ex);
        return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles binding errors when Spring cannot bind request parameters to objects.
     * Returns a BAD REQUEST response with field details.
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @SuppressWarnings("unused")
    public Map<String, Object> handleBindException(BindException ex) {
        FieldError error = ex.getFieldError();

        log.warn("Bind exception occurred on field '{}': {}", error != null ? error.getField() : "unknown", error != null ? error.getDefaultMessage() : "unknown");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "Invalid input");
        response.put("field", error != null ? error.getField() : null);
        response.put("rejectedValue", error != null ? error.getRejectedValue() : null);
        response.put("message", error != null ? error.getDefaultMessage() : "Invalid value");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("timestamp", OffsetDateTime.now().toString());

        return response;
    }

    /**
     * Handles InvalidSortFieldException when a sort field is invalid.
     * Returns a BAD REQUEST response with an error message.
     */
    @ExceptionHandler(InvalidSortFieldException.class)
    @SuppressWarnings("unused")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleInvalidSortField(InvalidSortFieldException ex,
                                                                      HttpServletRequest request) {
        Map<String, Object> body = Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "path", request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles HttpMessageNotReadableException when the request body is malformed or unreadable.
     * Returns a BAD REQUEST response with an error message.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @SuppressWarnings("unused")
    public Map<String, Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "Malformed JSON");
        response.put("message", "Request body is invalid or unreadable");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("timestamp", OffsetDateTime.now().toString());

        return response;
    }

    /**
     * Extracts the field name from a property path, which may contain nested properties.
     * For example, "user.name" will return "name".
     */
    private String extractFieldName(String path) {
        int lastDot = path.lastIndexOf(".");
        return lastDot != -1 ? path.substring(lastDot + 1) : path;
    }

    public record FieldValidationError(String field, Object rejectedValue, String message) {
    }
}