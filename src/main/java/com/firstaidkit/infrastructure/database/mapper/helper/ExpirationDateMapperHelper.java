package com.firstaidkit.infrastructure.database.mapper.helper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class ExpirationDateMapperHelper {

    // using in DrugMapper
    @Named("mapExpirationDateToYearMonth")
    public String mapExpirationDateToYearMonth(OffsetDateTime expirationDate) {
        return expirationDate.getYear() + "-" + String.format("%02d", expirationDate.getMonthValue());
    }
}