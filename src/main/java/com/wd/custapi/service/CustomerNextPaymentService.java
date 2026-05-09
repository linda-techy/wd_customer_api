package com.wd.custapi.service;

import com.wd.custapi.dto.NextPaymentMilestoneDto;
import com.wd.custapi.model.PaymentStage;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.enums.PaymentStageStatus;
import com.wd.custapi.repository.PaymentStageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

/**
 * Computes the customer's "next" payment milestone for the project-detail
 * card. "Next" = lowest {@code stage_number} whose status is NOT terminal
 * ({@code PAID}, {@code ON_HOLD}). Pure read.
 *
 * <p>{@link #getNextPaymentMilestoneAt(Project, LocalDate)} is the test seam
 * — production callers go through {@link #getNextPaymentMilestone(Project)}
 * which reads "today" in IST.
 *
 * <p>Authorisation is the caller's responsibility — the controller resolves
 * an authorised {@link Project} via {@code DashboardService.getProjectByUuidAndEmail}
 * and passes it in. The service does NOT re-resolve the UUID.
 */
@Service
public class CustomerNextPaymentService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private static final EnumSet<PaymentStageStatus> TERMINAL =
            EnumSet.of(PaymentStageStatus.PAID, PaymentStageStatus.ON_HOLD);

    private final PaymentStageRepository paymentStageRepository;

    public CustomerNextPaymentService(PaymentStageRepository paymentStageRepository) {
        this.paymentStageRepository = paymentStageRepository;
    }

    @Transactional(readOnly = true)
    public NextPaymentMilestoneDto getNextPaymentMilestone(Project project) {
        return getNextPaymentMilestoneAt(project, LocalDate.now(IST));
    }

    @Transactional(readOnly = true)
    public NextPaymentMilestoneDto getNextPaymentMilestoneAt(Project project, LocalDate today) {
        List<PaymentStage> stages = paymentStageRepository
                .findByProjectIdOrderByStageNumberAsc(project.getId());

        // Summary uses the full stage list — totals must reflect the entire
        // schedule, not just non-terminal rows.
        BigDecimal totalContractValue = stages.stream()
                .map(s -> nz(s.getStageAmountInclGst()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = stages.stream()
                .map(s -> nz(s.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Outstanding mirrors the "non-terminal" rule used by next-stage selection:
        // PAID is settled; ON_HOLD is excluded by product decision (the customer is
        // not currently expected to pay it, and surfacing it as outstanding would
        // make the summary inconsistent with the stage list shown above the card).
        BigDecimal totalOutstanding = stages.stream()
                .filter(s -> s.getStatus() != null && !TERMINAL.contains(s.getStatus()))
                .map(s -> nz(s.getNetPayableAmount()))
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NextPaymentMilestoneDto.Summary summary = new NextPaymentMilestoneDto.Summary(
                totalContractValue, totalPaid, totalOutstanding, stages.size());

        PaymentStage next = stages.stream()
                .filter(s -> s.getStatus() != null && !TERMINAL.contains(s.getStatus()))
                .findFirst()
                .orElse(null);

        if (next == null) {
            return new NextPaymentMilestoneDto(null, summary);
        }

        Integer daysUntilDue = next.getDueDate() == null
                ? null
                : (int) ChronoUnit.DAYS.between(today, next.getDueDate());

        BigDecimal netPayable = nz(next.getNetPayableAmount());
        BigDecimal percentOfContract = totalContractValue.compareTo(BigDecimal.ZERO) > 0
                ? netPayable.multiply(BigDecimal.valueOf(100))
                        .divide(totalContractValue, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        NextPaymentMilestoneDto.Stage stageDto = new NextPaymentMilestoneDto.Stage(
                next.getStageNumber(),
                next.getStageName(),
                next.getDueDate(),
                daysUntilDue,
                next.getStatus().name(),
                netPayable,
                nz(next.getStagePercentage()),
                percentOfContract,
                stages.size()
        );

        return new NextPaymentMilestoneDto(stageDto, summary);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
