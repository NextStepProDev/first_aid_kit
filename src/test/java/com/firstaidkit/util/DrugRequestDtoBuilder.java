package com.firstaidkit.util;

import com.firstaidkit.controller.dto.drug.DrugCreateRequest;

import java.time.OffsetDateTime;

public class DrugRequestDtoBuilder {
    static int currentYear = OffsetDateTime.now().getYear() + 1;

    public static DrugCreateRequest getValidDrugRequestDto() {
        return DrugCreateRequest.builder()
                .name("Aspirin")
                .form("PILLS")
                .description("A common pain reliever")
                .expirationYear(currentYear)
                .expirationMonth(5)
                .build();
    }
}
