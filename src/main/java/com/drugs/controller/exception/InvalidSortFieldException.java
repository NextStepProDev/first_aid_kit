package com.drugs.controller.exception;

public class InvalidSortFieldException extends RuntimeException {
    public InvalidSortFieldException(String field) {
        super("Invalid sort field: " + field);
    }
}