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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookIngestionServicePaymentMilestoneTest {

    @Mock CustomerNotificationRepository notificationRepository;
    @Mock CustomerUserRepository userRepository;
    @Mock PushNotificationService pushNotificationService;
    @Mock ReceivedWebhookEventRepository webhookEventRepository;

    @InjectMocks WebhookIngestionService service;

    private CustomerUser customer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectMapper",
                new ObjectMapper().registerModule(new JavaTimeModule()));

        customer = new CustomerUser();
        customer.setId(17L);
        customer.setEmail("alice@example.com");
        customer.setFirstName("Alice");
        customer.setFcmToken("fcm-token-abc");

        when(webhookEventRepository.save(any(ReceivedWebhookEvent.class)))
                .thenAnswer(inv -> {
                    ReceivedWebhookEvent r = inv.getArgument(0);
                    if (r.getStatus() == null) r.setStatus(ReceivedWebhookEvent.STATUS_PROCESSING);
                    return r;
                });
        when(userRepository.findCustomersByProjectId(42L))
                .thenReturn(java.util.List.of(customer));
    }

    private PortalWebhookEvent eventFor(String kind) {
        return new PortalWebhookEvent(
                PortalEventType.PAYMENT_MILESTONE_DUE,
                42L,                  // projectId
                null,                 // customerId — null fans out via project lookup
                113L,                 // referenceId = stageId
                "Payment reminder",
                Map.of(
                        "reminderKind", kind,
                        "stageId", "113",
                        "stageNumber", "4",
                        "stageName", "Plastering",
                        "dueDate", "2026-05-13",
                        "netPayableAmount", "425000.00"
                ),
                LocalDateTime.now());
    }

    @Test
    void process_tMinus3_titleAndBody() {
        service.process(eventFor("T_MINUS_3"));

        ArgumentCaptor<CustomerNotification> notif = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(notif.capture());

        assertEquals("PAYMENT_MILESTONE_DUE", notif.getValue().getNotificationType());
        assertEquals("Payment due in 3 days", notif.getValue().getTitle());
        // Body must use the existing ContractValueFormatter (₹4 L for 4,25,000 — formatINR rounds to 0 decimals at lakh scale).
        assertEquals("Stage 4 — Plastering (\u20B94 L)", notif.getValue().getBody());
    }

    @Test
    void process_dueToday_titleAndBody() {
        service.process(eventFor("DUE_TODAY"));

        ArgumentCaptor<CustomerNotification> notif = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(notif.capture());
        assertEquals("Payment due today", notif.getValue().getTitle());
        assertEquals("Stage 4 — Plastering (\u20B94 L)", notif.getValue().getBody());
    }

    @Test
    void process_overdue_titleAndBody() {
        service.process(eventFor("OVERDUE"));

        ArgumentCaptor<CustomerNotification> notif = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(notif.capture());
        assertEquals("Payment overdue", notif.getValue().getTitle());
        assertEquals("Stage 4 — Plastering (\u20B94 L)", notif.getValue().getBody());
    }

    @Test
    void process_paymentMilestone_dispatchesFcmWithDeepLinkPayments() {
        service.process(eventFor("DUE_TODAY"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String,String>> dataCap = ArgumentCaptor.forClass(Map.class);
        verify(pushNotificationService).sendToToken(
                eq("fcm-token-abc"), eq("Payment due today"),
                eq("Stage 4 — Plastering (\u20B94 L)"), dataCap.capture());

        Map<String, String> data = dataCap.getValue();
        assertEquals("payments", data.get("deepLink"));
        // type matches the customer-app's _handleTap switch keys:
        assertEquals("PAYMENT_MILESTONE_DUE", data.get("type"));
        // notificationType retained for backward-compat with anything else reading it:
        assertEquals("PAYMENT_MILESTONE_DUE", data.get("notificationType"));
        assertEquals("113", data.get("referenceId"));
    }

    @Test
    void process_paymentMilestone_smallAmountUsesIndianGrouping() {
        // 95,000 < 1 lakh → ContractValueFormatter falls through to "#,##,###" → "₹95,000".
        PortalWebhookEvent e = new PortalWebhookEvent(
                PortalEventType.PAYMENT_MILESTONE_DUE, 42L, null, 113L, "summary",
                Map.of("reminderKind", "DUE_TODAY", "stageId", "113",
                       "stageNumber", "4", "stageName", "Plastering",
                       "dueDate", "2026-05-13", "netPayableAmount", "95000.00"),
                LocalDateTime.now());

        service.process(e);

        ArgumentCaptor<CustomerNotification> notif = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(notif.capture());
        assertEquals("Stage 4 — Plastering (\u20B995,000)", notif.getValue().getBody());
    }
}
