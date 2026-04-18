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
        String reasonCategory,
        String responsibleParty,
        Integer durationDays,
        String impactDescription,
        boolean isOpen,
        long impactDays
) {
    public static CustomerDelayLogDto from(DelayLog d) {
        boolean open = d.getToDate() == null;
        LocalDate end = open ? LocalDate.now() : d.getToDate();
        long days = ChronoUnit.DAYS.between(d.getFromDate(), end);
        return new CustomerDelayLogDto(
                d.getId(), d.getDelayType(), d.getFromDate(), d.getToDate(),
                d.getReasonText(), d.getReasonCategory(), d.getResponsibleParty(),
                d.getDurationDays(), d.getImpactDescription(), open, days
        );
    }
}
