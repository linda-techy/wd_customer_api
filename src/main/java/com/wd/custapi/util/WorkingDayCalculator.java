package com.wd.custapi.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Minimal Mon–Sat working-day arithmetic for customer-API.
 *
 * <p>Does NOT consult the portal holiday calendar — customer-API does not
 * mirror that table. Accuracy trade-off documented in the S2 PR4 spec; for
 * exact handover precision callers should hit the portal-api equivalent.
 *
 * <p>Convention: Mon-Sat are counted as working days, Sunday is excluded.
 * The calculation is exclusive at the end date — i.e. workingDaysBetween(d, d)
 * returns 0, and the day at {@code start} is counted while the day at
 * {@code end} is not.
 */
public final class WorkingDayCalculator {

    private WorkingDayCalculator() {}

    /**
     * Returns the number of working days (Mon-Sat) between {@code start} and
     * {@code end}. Negative when {@code end} is before {@code start}; zero when
     * either argument is {@code null} or the dates are equal.
     */
    public static int workingDaysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        int sign = end.isBefore(start) ? -1 : 1;
        LocalDate from = sign > 0 ? start : end;
        LocalDate to = sign > 0 ? end : start;

        int count = 0;
        LocalDate cursor = from;
        while (cursor.isBefore(to)) {
            if (cursor.getDayOfWeek() != DayOfWeek.SUNDAY) count++;
            cursor = cursor.plusDays(1);
        }
        return sign * count;
    }
}
