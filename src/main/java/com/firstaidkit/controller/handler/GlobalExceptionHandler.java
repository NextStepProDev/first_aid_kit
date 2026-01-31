package com.firstaidkit.controller.handler;

import com.firstaidkit.controller.dto.error.ErrorMessage;
import com.firstaidkit.controller.dto.error.FieldValidationError;
import com.firstaidkit.controller.dto.error.ValidationErrorMessageDTO;
import com.firstaidkit.domain.exception.*;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Handles MethodArgumentNotValidException when a method argument validation fails (e.g., DTO validation).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorMessageDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<FieldValidationError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
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

        List<FieldValidationError> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(err -> new FieldValidationError(
                        err.getObjectName(),
                        null,
                        err.getDefaultMessage()
                ))
                .toList();

        List<FieldValidationError> errors = new java.util.ArrayList<>(fieldErrors);
        errors.addAll(globalErrors);

        log.warn("Validation error(s): {}", errors);

        ValidationErrorMessageDTO validationError = new ValidationErrorMessageDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                OffsetDateTime.now().toString(),
                errors
        );
        return ResponseEntity.badRequest().body(validationError);
    }

    // Handles MethodArgumentTypeMismatchException when a method argument type does not match the expected type (e.g. 1,5 to Integer).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorMessage> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value for parameter '%s': %s", ex.getName(), ex.getValue());
        log.warn("Type mismatch: {}", message);
        return ResponseEntity
                .badRequest()
                .body(new ErrorMessage(400, message));
    }


    @ExceptionHandler(DrugNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleDrugNotFound(DrugNotFoundException ex) {
        log.warn("Drug not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(new ErrorMessage(404, ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorMessage> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(404, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleGeneralException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessage(500, "An unexpected error occurred"));
    }


    @ExceptionHandler(InvalidSortFieldException.class)
    public ResponseEntity<ErrorMessage> handleInvalidSortFieldException(InvalidSortFieldException ex) {
        log.warn("Invalid sort field: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(400, "Unknown sort field: " + ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(400, "Request body is invalid or unreadable"));
    }

    // Handles IllegalArgumentException when an invalid argument is passed to a method.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessage> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(new ErrorMessage(400, ex.getMessage()));
    }

    // Handles EmailSendingException when there is an error sending an email.
    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<ErrorMessage> handleEmailSendingException(EmailSendingException ex) {
        log.error("Email sending failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessage(500, "Failed to send expiry alert email. Please try again later."));
    }

    // Handles MissingServletRequestParameterException when a required request parameter is missing.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorMessage> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        String message = "Missing required request parameter: " + ex.getParameterName();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(400, message));
    }

    // Handles ConstraintViolationException when a constraint on a bean property is violated. (@Valid, @Min, @Max, etc.)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessage> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failed";
        }
        log.warn("Constraint violation(s): {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(400, message));
    }

    // Handles invalid property references raised by Spring Data (e.g., bad sort field or derived query property)
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorMessage> handlePropertyReferenceException(PropertyReferenceException ex) {
        Class<?> rawType = ex.getType().getType();
        String entityType = rawType.getSimpleName();
        String property = ex.getPropertyName();
        String message = String.format("Invalid property reference '%s' for %s", property, entityType);
        log.warn("Property reference error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(400, message));
    }

    // e.g. /api/drugsf/
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorMessage> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorMessage(404, "Resource not found"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorMessage> handleDisabledException(DisabledException ex) {
        log.warn("Disabled account login attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorMessage(403, ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessage> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorMessage(401, ex.getMessage()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorMessage> handleAccountLocked(AccountLockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(new ErrorMessage(423, ex.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorMessage> handleInvalidPassword(InvalidPasswordException ex) {
        log.warn("Invalid password: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorMessage(401, ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorMessage> handleInvalidToken(InvalidTokenException ex) {
        log.warn("Invalid token: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(400, ex.getMessage()));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorMessage> handlePasswordMismatch(PasswordMismatchException ex) {
        log.warn("Password mismatch: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(400, ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorMessage> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorMessage(401, "Authentication failed"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorMessage(403, "Access denied - insufficient permissions"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorMessage> handleIllegalStateException(IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorMessage(500, ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorMessage> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorMessage(409, "Data conflict - resource may already exist"));
    }
}