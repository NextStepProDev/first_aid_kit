package com.firstaid.controller.dto.drug;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DrugFormDTO {
    GEL("Żel"),
    PILLS("Tabletki"),
    SYRUP("Syrop"),
    DROPS("Krople"),
    SUPPOSITORIES("Czopki"),
    SACHETS("Saszetki"),
    CREAM("Krem"),
    SPRAY("Spray"),
    OINTMENT("Maść"),
    LIQUID("Płyn"),
    POWDER("Proszek"),
    INJECTION("Zastrzyk"),
    BANDAGE("Bandaż"),
    INHALER("Inhalator"),
    PATCH("Plaster"),
    SOLUTION("Roztwór"),
    OTHER("Inne");

    private final String label;

    DrugFormDTO(String label) {
        this.label = label;
    }

    public static DrugFormDTO fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Drug form cannot be null");
        }

        return Arrays.stream(values())
                .filter(f -> f.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid drug form: " + value + ". Allowed: " + Arrays.toString(values())
                ));
    }
}