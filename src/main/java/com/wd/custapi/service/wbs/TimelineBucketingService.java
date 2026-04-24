package com.wd.custapi.service.wbs;

import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class TimelineBucketingService {

    public enum Bucket { WEEK, UPCOMING, COMPLETED }

    public interface TaskLike {
        LocalDate startDate();
        LocalDate endDate();
        String status();
        int progressPercent();
    }

    public record WeekBounds(LocalDate start, LocalDate end) {}

    public WeekBounds weekBounds(LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new WeekBounds(monday, monday.plusDays(6));
    }

    public Bucket classify(TaskLike task, LocalDate today) {
        if ("COMPLETED".equals(task.status()) || task.progressPercent() == 100) {
            return Bucket.COMPLETED;
        }
        WeekBounds w = weekBounds(today);
        if (task.startDate() == null || task.endDate() == null) {
            return Bucket.UPCOMING;
        }
        if (!task.startDate().isAfter(w.end()) && !task.endDate().isBefore(w.start())) {
            return Bucket.WEEK;
        }
        return Bucket.UPCOMING;
    }
}
