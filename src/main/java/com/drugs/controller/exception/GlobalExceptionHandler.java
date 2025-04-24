package com.drugs.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Obsługuje wyjątek, gdy lek nie został znaleziony
    @ExceptionHandler(DrugNotFoundException.class)
    public ResponseEntity<String> handleDrugNotFound(DrugNotFoundException ex) {
        // Logowanie błędu
        System.err.println("Error: " + ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // Obsługuje ogólne wyjątki
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        // Logowanie błędu
        System.err.println("Error: " + ex.getMessage());
        return new ResponseEntity<>("An error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}