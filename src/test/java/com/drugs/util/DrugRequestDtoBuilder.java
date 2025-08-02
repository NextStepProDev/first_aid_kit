package com.drugs.util;

import com.drugs.controller.dto.DrugRequestDTO;

import java.time.OffsetDateTime;

public class DrugRequestDtoBuilder {
    static int currentYear = OffsetDateTime.now().getYear() + 1;

    public static DrugRequestDTO getValidDrugRequestDto() {
        return DrugRequestDTO.builder()
                .name("Aspirin")
                .form("PILLS")
                .description("A common pain reliever")
                .expirationYear(currentYear)
                .expirationMonth(5)
                .build();
    }
}
