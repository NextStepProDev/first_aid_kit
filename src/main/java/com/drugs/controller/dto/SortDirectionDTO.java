package com.drugs.controller.dto;

import java.util.Arrays;

public enum SortDirectionDTO {
    ASC, DESC;

    public static SortDirectionDTO fromString(String value) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid value for parameter 'direction': " + value));
    }
}