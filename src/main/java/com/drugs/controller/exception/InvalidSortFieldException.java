package com.drugs.controller.exception;

public class InvalidSortFieldException extends RuntimeException {
    public InvalidSortFieldException(String field) {
        super(field);
    }
}