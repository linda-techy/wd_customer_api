package com.wd.custapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wd.custapi.dto.PortalEventType;
import com.wd.custapi.dto.PortalWebhookEvent;
import com.wd.custapi.model.CustomerNotification;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.ReceivedWebhookEvent;
import com.wd.custapi.repository.CustomerNotificationRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.ReceivedWebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookIngestionServiceTest {

    @Mock
    private CustomerNotificationRepository notificationRepository;

    @Mock
    private CustomerUserRepository userRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private ReceivedWebhookEventRepository webhookEventRepository;

    @InjectMocks
    private WebhookIngestionService webhookIngestionService;

    // Real ObjectMapper — needed for JSON serialisation in the service
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private CustomerUser customer;

    @BeforeEach
    void setUp() {
        // Inject real ObjectMapper via reflection so the service can serialise events
        org.springframework.test.util.ReflectionTestUtils.setField(
                webhookIngestionService, "objectMapper", objectMapper);

        customer = new CustomerUser();
        customer.setId(1L);
        customer.setEmail("john@example.com");
        customer.setFirstName("John");
        customer.setLastName("Doe");
    }

    // Helper: saves record with an id so subsequent saves work
    private ReceivedWebhookEvent savedRecord(String status) {
        ReceivedWebhookEvent record = new ReceivedWebhookEvent();
        record.setStatus(status);
        return record;
    }

    // ── process — event-type routing ──────────────────────────────────────────

    @Test
    void process_siteReportSubmitted_createsNotificationWithSiteReportType() throws Exception {
        PortalWebhookEvent event = new PortalWebhookEvent(
                PortalEventType.SITE_REPORT_SUBMITTED, 10L, 1L, 55L,
                "New site report", Map.of("reportTitle", "Week 3 Report"), LocalDateTime.now());

        ReceivedWebhookEvent record = savedRecord(ReceivedWebhookEvent.STATUS_PROCESSING);
        when(webhookEventRepository.save(any(ReceivedWebhookEvent.class))).thenReturn(record);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        webhookIngestionService.process(event);

        ArgumentCaptor<CustomerNotification> notifCaptor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        CustomerNotification notif = notifCaptor.getValue();
        assertEquals("SITE_REPORT", notif.getNotificationType());
        assertTrue(notif.getTitle().contains("Week 3 Report"));
        assertEquals(customer, notif.getCustomerUser());
    }

    @Test
    void process_invoiceIssued_createsPaymentNotification() throws Exception {
        PortalWebhookEvent event = new PortalWebhookEvent(
                PortalEventType.INVOICE_ISSUED, 10L, 1L, 20L,
                "Invoice issued", Map.of("invoiceNumber", "INV-001"), LocalDateTime.now());

        ReceivedWebhookEvent record = savedRecord(ReceivedWebhookEvent.STATUS_PROCESSING);
        when(webhookEventRepository.save(any(ReceivedWebhookEvent.class))).thenReturn(record);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        webhookIngestionService.process(event);

        ArgumentCaptor<CustomerNotification> notifCaptor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertEquals("PAYMENT", notifCaptor.getValue().getNotificationType());
        assertTrue(notifCaptor.getValue().getTitle().contains("INV-001"));
    }

    @Test
    void process_paymentRecorded_createsPaymentNotification() throws Exception {
        PortalWebhookEvent event = new PortalWebhookEvent(
                PortalEventType.PAYMENT_RECORDED, 10L, 1L, 30L,
                "Payment recorded", Map.of("amount", "50000"), LocalDateTime.now());

        ReceivedWebhookEvent record = savedRecord(ReceivedWebhookEvent.STATUS_PROCESSING);
        when(webhookEventRepository.save(any(ReceivedWebhookEvent.class))).thenReturn(record);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        webhookIngestionService.process(event);

        ArgumentCaptor<CustomerNotification> notifCaptor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertEquals("PAYMENT", notifCaptor.getValue().getNotificationType());
        assertTrue(notifCaptor.getValue().getTitle().contains("50000"));
    }

    @Test
    void process_delayReported_createsDelayNotificationWithCategory() throws Exception {
        PortalWebhookEvent event = new PortalWebhookEvent(
                PortalEventType.DELAY_REPORTED, 10L, 1L, 40L,
                "Delay reported", Map.of("category", "MATERIAL"), LocalDateTime.now());

        ReceivedWebhookEvent record = savedRecord(ReceivedWebhookEvent.STATUS_PROCESSING);
        when(webhookEventRepository.save(any(ReceivedWebhookEvent.class))).thenReturn(record);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        webhookIngestionService.process(event);

        ArgumentCaptor<CustomerNotification> notifCaptor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        assertEquals("DELAY", notifCaptor.getValue().getNotificationType());
        assertTrue(notifCaptor.getValue().getTitle().contains("MATERIAL"));
    }

    // ── process — status tracking ─────────────────────────────────────────────

    @Test
    void process_successfulProcessing_savesProcessingThenProcessedStatus() throws Exception {
        PortalWebhookEvent event = new PortalWebhookEvent(
                PortalEventType.INVOICE_ISSUED, 10L, 1L, 20L,
                "Invoice", Map.of("invoiceNumber", "INV-002"), LocalDateTime.now());

        ReceivedWebhookEvent record = savedRecord(ReceivedWebhookEvent.STATUS_PROCESSING);
        when(webhookEventRepository.save(any(ReceivedWebhookEvent.class))).thenReturn(record);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        webhookIngestionService.process(event);

        // First save: PROCESSING; second save: PROCESSED
        ArgumentCaptor<ReceivedWebhookEvent> captor = ArgumentCaptor.forClass(ReceivedWebhookEvent.class);
        verify(webhookEventRepository, atLeast(2)).save(captor.capture());

        List<ReceivedWebhookEvent> saves = captor.getAllValues();
        // The last save should have PROCESSED status set on the record
        ReceivedWebhookEvent lastSaved = saves.get(saves.size() - 1);
        assertEquals(ReceivedWebhookEvent.STATUS_PROCESSED, lastSaved.getStatus());
    }

    @Test
    void process_onFailure_savesFailedStatusWithErrorMessage() throws Exception {
        PortalWebhookEvent event = new PortalWebhookEvent(
                PortalEventType.INVOICE_ISSUED, 10L, 1L, 20L,
                "Invoice", Map.of("invoiceNumber", "INV-003"), LocalDateTime.now());

        ReceivedWebhookEvent record = savedRecord(ReceivedWebhookEvent.STATUS_PROCESSING);
        when(webhookEventRepository.save(any(ReceivedWebhookEvent.class))).thenReturn(record);
        // Cause failure: user lookup throws
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("DB error"));

        webhookIngestionService.process(event);

        ArgumentCaptor<ReceivedWebhookEvent> captor = ArgumentCaptor.forClass(ReceivedWebhookEvent.class);
        verify(webhookEventRepository, atLeast(2)).save(captor.capture());

        List<ReceivedWebhookEvent> saves = captor.getAllValues();
        ReceivedWebhookEvent lastSaved = saves.get(saves.size() - 1);
        assertEquals(ReceivedWebhookEvent.STATUS_FAILED, lastSaved.getStatus());
        assertNotNull(lastSaved.getErrorMessage());
    }

    // ── retryFailedEvents ─────────────────────────────────────────────────────

    @Test
    void retryFailedEvents_retriesFailedEventsUpTo3Times() throws Exception {
        // Build a serialized event payload
        PortalWebhookEvent event = new PortalWebhookEvent(
                PortalEventType.PHASE_UPDATED, 10L, 1L, 5L,
                "Phase update", Map.of("phaseName", "Foundation"), LocalDateTime.now());
        String payload = objectMapper.writeValueAsString(event);

        ReceivedWebhookEvent failedRecord = new ReceivedWebhookEvent();
        failedRecord.setEventType("PHASE_UPDATED");
        failedRecord.setStatus(ReceivedWebhookEvent.STATUS_FAILED);
        failedRecord.setPayload(payload);
        failedRecord.setAttemptCount(0);

        when(webhookEventRepository.findByStatus(ReceivedWebhookEvent.STATUS_FAILED))
                .thenReturn(List.of(failedRecord));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(webhookEventRepository.save(any(ReceivedWebhookEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        webhookIngestionService.retryFailedEvents();

        // Attempt count incremented and status set to PROCESSED on success
        assertEquals(1, failedRecord.getAttemptCount());
        assertEquals(ReceivedWebhookEvent.STATUS_PROCESSED, failedRecord.getStatus());
    }

    @Test
    void retryFailedEvents_abandonsEventsExceeding3Attempts() throws Exception {
        PortalWebhookEvent event = new PortalWebhookEvent(
                PortalEventType.PHASE_UPDATED, 10L, 1L, 5L,
                "Phase update", Map.of("phaseName", "Foundation"), LocalDateTime.now());
        String payload = objectMapper.writeValueAsString(event);

        ReceivedWebhookEvent exhaustedRecord = new ReceivedWebhookEvent();
        exhaustedRecord.setEventType("PHASE_UPDATED");
        exhaustedRecord.setStatus(ReceivedWebhookEvent.STATUS_FAILED);
        exhaustedRecord.setPayload(payload);
        exhaustedRecord.setAttemptCount(3); // already at max
        exhaustedRecord.setErrorMessage("Previous error");

        when(webhookEventRepository.findByStatus(ReceivedWebhookEvent.STATUS_FAILED))
                .thenReturn(List.of(exhaustedRecord));
        when(webhookEventRepository.save(any(ReceivedWebhookEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        webhookIngestionService.retryFailedEvents();

        // Should not attempt to process — error message should be prefixed with [ABANDONED]
        assertTrue(exhaustedRecord.getErrorMessage().startsWith("[ABANDONED"));
        verify(notificationRepository, never()).save(any());
    }
}
