package com.wd.custapi.dto;

import com.wd.custapi.model.DelayLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the customer-safe delay log projection.
 *
 * These tests exist to lock in the privacy contract: the raw internal fields
 * (reason_text, responsible_party, impact_description) must NEVER end up on
 * the DTO a customer receives — even if the underlying entity exposes them.
 */
class CustomerDelayLogDtoTest {

    @Test
    void from_mapsCustomerSafeFields() {
        DelayLog d = mock(DelayLog.class);
        when(d.getId()).thenReturn(42L);
        when(d.getDelayType()).thenReturn("WEATHER");
        when(d.getReasonCategory()).thenReturn("WEATHER");
        when(d.getFromDate()).thenReturn(LocalDate.of(2026, 4, 10));
        when(d.getToDate()).thenReturn(LocalDate.of(2026, 4, 13));
        when(d.getDurationDays()).thenReturn(3);
        when(d.getCustomerSummary())
                .thenReturn("Pour rescheduled due to rainfall; no impact on handover.");
        when(d.getImpactOnHandover()).thenReturn("NONE");

        CustomerDelayLogDto dto = CustomerDelayLogDto.from(d);

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.delayType()).isEqualTo("WEATHER");
        assertThat(dto.reasonCategory()).isEqualTo("WEATHER");
        assertThat(dto.fromDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(dto.toDate()).isEqualTo(LocalDate.of(2026, 4, 13));
        assertThat(dto.durationDays()).isEqualTo(3);
        assertThat(dto.customerSummary())
                .isEqualTo("Pour rescheduled due to rainfall; no impact on handover.");
        assertThat(dto.impactOnHandover()).isEqualTo("NONE");
    }

    @Test
    void from_doesNotExposeInternalFields_evenIfEntityHasThem() {
        // This is the core privacy contract. Even though the entity knows the
        // reason text, responsible party, and impact description, the DTO
        // has no fields for them — there is no way to surface them.
        DelayLog d = mock(DelayLog.class);
        when(d.getId()).thenReturn(1L);
        when(d.getDelayType()).thenReturn("SUBCONTRACTOR");
        when(d.getFromDate()).thenReturn(LocalDate.now());
        // Entity has all the internal noise set:
        when(d.getReasonText())
                .thenReturn("Subcontractor Acme Ltd walked off site after payment dispute");
        when(d.getResponsibleParty()).thenReturn("Acme Ltd");
        when(d.getImpactDescription()).thenReturn("Blocks 3rd floor pour until replacement");
        when(d.getCustomerSummary()).thenReturn("Crew change underway; 2-day delay expected.");

        CustomerDelayLogDto dto = CustomerDelayLogDto.from(d);

        // The DTO exposes only the curated summary.
        assertThat(dto.customerSummary()).isEqualTo("Crew change underway; 2-day delay expected.");

        // Compile-time guarantee: the DTO record has no accessors for these fields,
        // so we prove absence with reflection over the record's component names.
        var componentNames = java.util.Arrays.stream(CustomerDelayLogDto.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();
        assertThat(componentNames).doesNotContain(
                "reasonText", "responsibleParty", "impactDescription");
    }

    @Test
    void from_openDelay_setsIsOpenAndComputesImpactDaysFromToday() {
        DelayLog d = mock(DelayLog.class);
        when(d.getId()).thenReturn(1L);
        when(d.getDelayType()).thenReturn("MATERIAL_DELAY");
        when(d.getFromDate()).thenReturn(LocalDate.now().minusDays(5));
        when(d.getToDate()).thenReturn(null);

        CustomerDelayLogDto dto = CustomerDelayLogDto.from(d);

        assertThat(dto.isOpen()).isTrue();
        assertThat(dto.toDate()).isNull();
        assertThat(dto.impactDays()).isEqualTo(5L);
    }

    @Test
    void from_closedDelay_computesImpactDaysFromRange() {
        DelayLog d = mock(DelayLog.class);
        when(d.getId()).thenReturn(1L);
        when(d.getDelayType()).thenReturn("WEATHER");
        when(d.getFromDate()).thenReturn(LocalDate.of(2026, 4, 1));
        when(d.getToDate()).thenReturn(LocalDate.of(2026, 4, 8));

        CustomerDelayLogDto dto = CustomerDelayLogDto.from(d);

        assertThat(dto.isOpen()).isFalse();
        assertThat(dto.impactDays()).isEqualTo(7L);
    }
}
