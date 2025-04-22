package com.drugs.infrastructure.util;

import com.drugs.controller.dto.DrugsDTO;

import java.time.*;
import java.util.List;

public class DateUtils {

    private static final ZoneId EUROPE_WARSAW = ZoneId.of("Europe/Warsaw");

    public static OffsetDateTime buildExpirationDate(int year, int month) {
        return YearMonth.of(year, month)
                .atEndOfMonth()
                .atStartOfDay(EUROPE_WARSAW)
                .toOffsetDateTime();
    }

    public static OffsetDateTime buildStartOfMonth(int year, int month) {
        LocalDate firstDay = YearMonth.of(year, month).atDay(1);
        return firstDay.atStartOfDay().atOffset(ZoneOffset.UTC);
    }
}