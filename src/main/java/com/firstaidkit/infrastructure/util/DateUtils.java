package com.firstaidkit.infrastructure.util;

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

    public static OffsetDateTime startOfToday() {
        return OffsetDateTime.now(EUROPE_WARSAW)
                .toLocalDate()
                .atStartOfDay(EUROPE_WARSAW)
                .toOffsetDateTime();
    }
}