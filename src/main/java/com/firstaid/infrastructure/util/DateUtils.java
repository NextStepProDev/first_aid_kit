package com.firstaid.infrastructure.util;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

public class DateUtils {

    private static final ZoneId EUROPE_WARSAW = ZoneId.of("Europe/Warsaw");

    public static OffsetDateTime buildExpirationDate(int year, int month) {
        return YearMonth.of(year, month)
                .atEndOfMonth()
                .atStartOfDay(EUROPE_WARSAW)
                .toOffsetDateTime();
    }
}