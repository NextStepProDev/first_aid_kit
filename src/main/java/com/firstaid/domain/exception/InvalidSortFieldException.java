package com.firstaid.domain.exception;

public class InvalidSortFieldException extends RuntimeException {
    public InvalidSortFieldException(String field) {
        super(field);
    }
}
