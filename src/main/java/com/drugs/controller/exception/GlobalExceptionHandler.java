package com.drugs.controller.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles validation errors for DTOs annotated with @Valid.
     * Returns a detailed error message with field validation errors.
     * <p>
     * This method is triggered automatically by Spring when a DTO fails validation.
     * It collects all field errors and returns them in a structured format.
     * </p>
     * <p>
     * Note: If any DTO field violates annotations like @NotBlank, @Min, @Pattern, or custom validations
     * (e.g., @ValidDrugsForm), Spring will throw MethodArgumentNotValidException before reaching your controller!
     */
//    Jeśli jakiekolwiek pole DTO łamie adnotacje @NotBlank, @Min, @Pattern, lub niestandardową walidację
//    (@ValidDrugsForm) to Spring automatycznie rzuca MethodArgumentNotValidException, zanim nawet wejdzie do Twojego
//    kontrolera!
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<ValidationErrorMessageDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        err -> new FieldValidationError(
                                err.getField(),
                                err.getRejectedValue(),
                                err.getDefaultMessage()
                        ),
                        (first, second) -> first
                ))
                .values()
                .stream()
                .toList();

        log.error("handleValidationErrors - Validation error occurred: {}", errors);

        log.warn("Validation error occurred: {}", errors);

        ValidationErrorMessageDTO validationError = new ValidationErrorMessageDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                OffsetDateTime.now().toString(),
                errors
        );
        return ResponseEntity.badRequest().body(validationError);
    }

    /**
     * Handles type mismatch errors for request parameters (e.g. @RequestParam, @PathVariable).
     * Returns a BAD REQUEST response with an error message.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @SuppressWarnings("unused")
    public ResponseEntity<ErrorMessage> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value for parameter '%s': %s", ex.getName(), ex.getValue());
        log.error("handleTypeMismatch - {}", message);
        return ResponseEntity
                .badRequest()
                .body(new ErrorMessage(400, message));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<ErrorMessage> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ex.getMessage();
        String message = ex.getMessage();
        log.error("handleHandlerMethodValidationException - Validation error: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(400, message));
    }

//    /**
//     * Handles constraint violations for request parameters (e.g. @RequestParam, @PathVariable).
//     * Returns a list of field validation errors.
//     */
    // ❌ NOT used in Spring Boot 3.1+ automatic validation anymore
    // TODO: Remove if not used manually in your service/logic
//    @ExceptionHandler(ConstraintViolationException.class)
//    @SuppressWarnings("unused")
//    public ResponseEntity<ErrorMessage> handleConstraintViolation(ConstraintViolationException ex) {
//        log.warn("Constraint violation handler triggered: {}", ex.getMessage());
//
//        String combinedMessage = ex.getConstraintViolations().stream()
//                .map(violation -> {
//                    String field = extractFieldName(violation.getPropertyPath().toString());
//                    return field + ": " + violation.getMessage();
//                })
//                .collect(Collectors.joining("; "));
//
//        return ResponseEntity.badRequest()
//                .body(new ErrorMessage(400, combinedMessage));
//    }
//
    /**
     * Handles DrugNotFoundException when a requested drug does not exist.
     * Returns a 404 NOT FOUND status with an error message.
     */
    @ExceptionHandler(DrugNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleDrugNotFound(DrugNotFoundException ex) {
        log.warn("Drug not found: {}", ex.getMessage());
        log.error("handleDrugNotFound - Drug not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(new ErrorMessage(404, ex.getMessage()));
    }

    /**
     * Handles all uncaught exceptions and returns a generic 500 INTERNAL SERVER ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        log.error("Unhandled exception caught: ", ex);
        log.error("handleGeneralException - Unhandled exception caught: ", ex);
        return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }

//    /**
//     * Handles binding errors when Spring cannot bind request parameters to objects.
//     * Returns a BAD REQUEST response with field details.
//     */
    // ❌ Only used for form-like object binding (@ModelAttribute) — which you're not using
// TODO: Remove if not using @ModelAttribute binding

//    @ExceptionHandler(BindException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    @SuppressWarnings("unused")
//    public Map<String, Object> handleBindException(BindException ex) {
//        FieldError error = ex.getFieldError();
//
//        log.warn("Bind exception occurred on field '{}': {}", error != null ? error.getField() : "unknown", error != null ? error.getDefaultMessage() : "unknown");
//
//        Map<String, Object> response = new LinkedHashMap<>();
//        response.put("error", "Invalid input");
//        response.put("field", error != null ? error.getField() : null);
//        response.put("rejectedValue", error != null ? error.getRejectedValue() : null);
//        response.put("message", error != null ? error.getDefaultMessage() : "Invalid value");
//        response.put("status", HttpStatus.BAD_REQUEST.value());
//        response.put("timestamp", OffsetDateTime.now().toString());
//
//        return response;
//    }

    /**
     * Handles InvalidSortFieldException when an unknown sort field is requested.
     * Returns a BAD REQUEST response with an error message.
     */
    @ExceptionHandler(InvalidSortFieldException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<ErrorMessage> handleInvalidSortFieldException(InvalidSortFieldException ex) {
        log.warn("Invalid sort field: {}", ex.getMessage());
        log.error("handleInvalidSortFieldException - Invalid sort field: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(400, "Unknown sort field: " + ex.getMessage()));
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
        log.error("handleHttpMessageNotReadable - Malformed JSON request: {}", ex.getMessage());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "Malformed JSON");
        response.put("message", "Request body is invalid or unreadable");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("timestamp", OffsetDateTime.now().toString());

        return response;
    }

    /**
     * Handles IllegalArgumentException when an invalid argument is passed to a method.
     * Returns a BAD REQUEST response with an error message.
     */
    @SuppressWarnings("unused")
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessage> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("handleIllegalArgument - {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(new ErrorMessage(400, ex.getMessage()));
    }

    /**
     * Handles EmailSendingException when there is an error sending an email.
     * Returns a 500 INTERNAL SERVER ERROR with a generic error message.
     */
    @ExceptionHandler(EmailSendingException.class)
    @SuppressWarnings("unused")
    public ResponseEntity<ErrorMessage> handleEmailSendingException(EmailSendingException ex) {
        log.error("Email sending failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessage(500, "Failed to send expiry alert email. Please try again later."));
    }
//
//    /**
//     * Extracts the field name from a property path, which may contain nested properties.
//     * For example, "user.name" will return "name".
//     */
//    private String extractFieldName(String path) {
//        int lastDot = path.lastIndexOf(".");
//        return lastDot != -1 ? path.substring(lastDot + 1) : path;
//    }
}