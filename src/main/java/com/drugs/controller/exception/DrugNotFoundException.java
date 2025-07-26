package com.drugs.controller.exception;

public class DrugNotFoundException extends RuntimeException {

    // Constructor accepting a message
    public DrugNotFoundException(String message) {
        super(message);
    }
}