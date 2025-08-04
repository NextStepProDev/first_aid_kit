package com.drugs.controller.dto;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SortFieldDTO {
    NAME("name"),
    FORM("form"),
    EXPIRATION_DATE("expirationDate"),
    DESCRIPTION("description");

    private final String fieldName;

    SortFieldDTO(String fieldName) {
        this.fieldName = fieldName;
    }

    public static SortFieldDTO fromString(String value) {
        return Arrays.stream(values())
                .filter(f -> f.fieldName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid value for parameter 'sortBy': " + value));
    }
}