package com.drugs.controller.exception;

public class DrugNotFoundException extends RuntimeException {

    // Constructor accepting a message
    public DrugNotFoundException(String message) {
        super(message);
    }

    // Constructor accepting a message and cause
    public DrugNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}