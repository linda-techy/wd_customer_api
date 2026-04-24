package com.wd.custapi.service.wbs;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProgressRollupService {

    public record TaskInput(int progressPercent, int weight) {}
    public record MilestoneInput(BigDecimal effectiveProgress, BigDecimal weight, String source) {}

    public BigDecimal rollupMilestone(List<TaskInput> tasks) {
        if (tasks == null || tasks.isEmpty()) return BigDecimal.ZERO;
        long totalWeight = tasks.stream().mapToLong(TaskInput::weight).sum();
        if (totalWeight == 0) return BigDecimal.ZERO;
        long weightedSum = tasks.stream()
                .mapToLong(t -> (long) t.progressPercent() * t.weight())
                .sum();
        return BigDecimal.valueOf(weightedSum)
                .divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal rollupProject(List<MilestoneInput> milestones) {
        if (milestones == null || milestones.isEmpty()) return BigDecimal.ZERO;
        BigDecimal totalWeight = milestones.stream()
                .map(MilestoneInput::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() == 0) return BigDecimal.ZERO;
        BigDecimal weightedSum = milestones.stream()
                .map(m -> m.effectiveProgress().multiply(m.weight()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }
}
