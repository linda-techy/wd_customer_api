package com.wd.custapi.dto;

import java.time.LocalDate;

/**
 * Customer-facing expected-handover payload — single GET response from
 * {@code /api/customer/projects/{uuid}/expected-handover}.
 *
 * <ul>
 *   <li>{@code projectFinishDate} — latest CPM-computed task ef_date for the
 *       project, or {@code null} when CPM has not yet run.</li>
 *   <li>{@code baselineFinishDate} — the approved-baseline finish date if a
 *       baseline exists for the project, otherwise {@code null}.</li>
 *   <li>{@code weeksRemaining} — Mon-Sat working weeks from "today" to
 *       {@code projectFinishDate}; {@code null} when {@code projectFinishDate}
 *       is {@code null}.</li>
 *   <li>{@code hasMaterialDelay} — {@code true} iff at least one
 *       customer-visible delay log on the project carries
 *       {@code impact_on_handover='MATERIAL'}; always present.</li>
 * </ul>
 */
public record ExpectedHandoverDto(
        LocalDate projectFinishDate,
        LocalDate baselineFinishDate,
        Integer weeksRemaining,
        boolean hasMaterialDelay
) {}
