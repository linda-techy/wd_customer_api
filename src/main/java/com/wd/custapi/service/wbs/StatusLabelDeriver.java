package com.wd.custapi.service.wbs;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class StatusLabelDeriver {

    public enum Label { ON_TRACK, ON_SCHEDULE, DELAYED, AT_RISK }

    public Label derive(LocalDate plannedStart, LocalDate plannedEnd, LocalDate today, int progressPercent) {
        if (plannedEnd != null && today.isAfter(plannedEnd)) {
            return progressPercent == 100 ? Label.ON_SCHEDULE : Label.DELAYED;
        }
        if (progressPercent == 100) {
            return Label.ON_SCHEDULE;
        }
        if (plannedStart == null || plannedEnd == null || today.isBefore(plannedStart)) {
            return Label.ON_TRACK;
        }
        long totalDays = ChronoUnit.DAYS.between(plannedStart, plannedEnd);
        if (totalDays <= 0) {
            return Label.ON_TRACK;
        }
        long elapsedDays = ChronoUnit.DAYS.between(plannedStart, today);
        double elapsedPct = (elapsedDays * 100.0) / totalDays;
        if (elapsedPct >= 10 && progressPercent < (elapsedPct * 0.5)) {
            return Label.AT_RISK;
        }
        return Label.ON_TRACK;
    }
}
