package com.wd.custapi.dto;

import com.wd.custapi.model.DelayLog;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record CustomerDelayLogDto(
        Long id,
        String delayType,
        LocalDate fromDate,
        LocalDate toDate,
        String reasonText,
        boolean isOpen,
        long impactDays
) {
    public static CustomerDelayLogDto from(DelayLog d) {
        boolean open = d.getToDate() == null;
        LocalDate end = open ? LocalDate.now() : d.getToDate();
        long days = ChronoUnit.DAYS.between(d.getFromDate(), end);
        return new CustomerDelayLogDto(
                d.getId(), d.getDelayType(), d.getFromDate(), d.getToDate(),
                d.getReasonText(), open, days
        );
    }
}
