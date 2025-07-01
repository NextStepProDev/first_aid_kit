package com.drugs.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DrugNotFoundException.class)
    public ResponseEntity<String> handleDrugNotFound(DrugNotFoundException ex) {
        ex.getMessage();
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

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();

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