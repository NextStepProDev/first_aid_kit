package com.drugs.controller.dto;

import lombok.Getter;

@Getter
public enum DrugsFormDTO {
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
    OTHER("Inne");

    private final String label;

    DrugsFormDTO(String label) {
        this.label = label;
    }
}