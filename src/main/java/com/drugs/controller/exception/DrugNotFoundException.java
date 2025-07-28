package com.drugs.controller.exception;

public class DrugNotFoundException extends RuntimeException {

    public DrugNotFoundException(String message) {
        super(message);
    }
}