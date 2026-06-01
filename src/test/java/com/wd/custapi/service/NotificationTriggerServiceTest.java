package com.wd.custapi.service;

import com.wd.custapi.model.CustomerNotification;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CustomerNotificationRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.PaymentScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure-Mockito unit tests for {@link NotificationTriggerService}. No Spring / DB.
 * Covers every trigger path (payment-due batch, query-replied, observation-resolved,
 * BOQ approval/change), the saveAndPush persistence + FCM fan-out, the "skip push when
 * no token" branch, and the must-never-throw exception guards.
 */
@ExtendWith(MockitoExtension.class)
class NotificationTriggerServiceTest {

    @Mock private CustomerNotificationRepository notificationRepository;
    @Mock private CustomerUserRepository customerUserRepository;
    @Mock private PaymentScheduleRepository paymentScheduleRepository;
    @Mock private PushNotificationService pushNotificationService;

    @InjectMocks private NotificationTriggerService service;

    private CustomerUser userWithToken(Long id, String token) {
        CustomerUser u = mock(CustomerUser.class);
        lenient().when(u.getId()).thenReturn(id);
        lenient().when(u.getFcmToken()).thenReturn(token);
        return u;
    }

    // ── sendPaymentDueReminders ───────────────────────────────────────────────

    @Test
    void sendPaymentDueReminders_savesNotificationAndPushesForEachRow() {
        Object[] row = new Object[]{ 3L, 88L, "Foundation milestone", new BigDecimal("150000"), 50L };
        when(paymentScheduleRepository.findDueOn(LocalDate.now().plusDays(3)))
                .thenReturn(List.<Object[]>of(row));
        CustomerUser user = userWithToken(3L, "fcm-token-abc");
        when(customerUserRepository.findById(3L)).thenReturn(Optional.of(user));

        service.sendPaymentDueReminders();

        ArgumentCaptor<CustomerNotification> captor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(captor.capture());
        CustomerNotification n = captor.getValue();
        assertThat(n.getNotificationType()).isEqualTo("PAYMENT_DUE");
        assertThat(n.getProjectId()).isEqualTo(50L);
        assertThat(n.getReferenceId()).isEqualTo(88L);
        assertThat(n.getTitle()).isEqualTo("Payment Due in 3 Days");
        assertThat(n.getBody()).contains("Foundation milestone");
        verify(pushNotificationService).sendToToken(eq("fcm-token-abc"),
                eq("Payment Due in 3 Days"), anyString(), anyMap());
    }

    @Test
    void sendPaymentDueReminders_nullProjectId_handledAndUserMissingSkipped() {
        Object[] rowNullProject = new Object[]{ 4L, 9L, "Slab", new BigDecimal("1000"), null };
        when(paymentScheduleRepository.findDueOn(any(LocalDate.class)))
                .thenReturn(List.<Object[]>of(rowNullProject));
        when(customerUserRepository.findById(4L)).thenReturn(Optional.empty());

        service.sendPaymentDueReminders();

        verify(notificationRepository, never()).save(any());
        verify(pushNotificationService, never()).sendToToken(any(), any(), any(), any());
    }

    @Test
    void sendPaymentDueReminders_oneBadRowDoesNotAbortBatch() {
        Object[] bad = new Object[]{ "not-a-number", 1L, "x", BigDecimal.ONE, 1L };
        Object[] good = new Object[]{ 5L, 2L, "Good", new BigDecimal("500"), 50L };
        when(paymentScheduleRepository.findDueOn(any(LocalDate.class)))
                .thenReturn(List.of(bad, good));
        CustomerUser goodUser = userWithToken(5L, null);
        when(customerUserRepository.findById(5L))
                .thenReturn(Optional.of(goodUser));

        assertThatCode(() -> service.sendPaymentDueReminders()).doesNotThrowAnyException();

        verify(notificationRepository, times(1)).save(any()); // only the good row persisted
    }

    @Test
    void sendPaymentDueReminders_repositoryThrows_swallowed() {
        when(paymentScheduleRepository.findDueOn(any(LocalDate.class)))
                .thenThrow(new RuntimeException("query exploded"));
        assertThatCode(() -> service.sendPaymentDueReminders()).doesNotThrowAnyException();
        verifyNoInteractions(notificationRepository);
    }

    // ── notifyQueryReplied ────────────────────────────────────────────────────

    @Test
    void notifyQueryReplied_persistsAndPushes() {
        CustomerUser user = userWithToken(3L, "tok");
        when(customerUserRepository.findById(3L)).thenReturn(Optional.of(user));

        service.notifyQueryReplied(3L, 50L, 77L, "When is the slab pour?");

        ArgumentCaptor<CustomerNotification> captor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo("QUERY_REPLIED");
        assertThat(captor.getValue().getBody()).contains("When is the slab pour?");
        verify(pushNotificationService).sendToToken(eq("tok"), eq("Query Replied"), anyString(), anyMap());
    }

    @Test
    void notifyQueryReplied_userNotFound_noop() {
        when(customerUserRepository.findById(3L)).thenReturn(Optional.empty());
        service.notifyQueryReplied(3L, 50L, 77L, "title");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifyQueryReplied_exceptionSwallowed() {
        when(customerUserRepository.findById(3L)).thenThrow(new RuntimeException("boom"));
        assertThatCode(() -> service.notifyQueryReplied(3L, 50L, 77L, "t")).doesNotThrowAnyException();
    }

    // ── notifyObservationResolved ─────────────────────────────────────────────

    @Test
    void notifyObservationResolved_persistsAndPushes() {
        CustomerUser user = userWithToken(3L, "tok");
        when(customerUserRepository.findById(3L)).thenReturn(Optional.of(user));

        service.notifyObservationResolved(3L, 50L, 22L, "Cracked tile");

        ArgumentCaptor<CustomerNotification> captor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getNotificationType()).isEqualTo("OBSERVATION_RESOLVED");
        assertThat(captor.getValue().getBody()).contains("Cracked tile");
        verify(pushNotificationService).sendToToken(eq("tok"), eq("Issue Resolved"), anyString(), anyMap());
    }

    @Test
    void notifyObservationResolved_exceptionSwallowed() {
        when(customerUserRepository.findById(anyLong())).thenThrow(new RuntimeException("boom"));
        assertThatCode(() -> service.notifyObservationResolved(3L, 50L, 22L, "t"))
                .doesNotThrowAnyException();
    }

    // ── notifyBoqApprovalAction ───────────────────────────────────────────────

    @Test
    void notifyBoqApprovalAction_approved_buildsApprovalNotification() {
        CustomerUser customer = userWithToken(3L, "tok");
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(50L);
        when(project.getName()).thenReturn("Villa A");

        service.notifyBoqApprovalAction(customer, project, "APPROVED", null);

        ArgumentCaptor<CustomerNotification> captor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(captor.capture());
        CustomerNotification n = captor.getValue();
        assertThat(n.getNotificationType()).isEqualTo("BOQ_APPROVED");
        assertThat(n.getTitle()).isEqualTo("BOQ Approval Confirmed");
        assertThat(n.getBody()).contains("Villa A");
        verify(pushNotificationService).sendToToken(eq("tok"), eq("BOQ Approval Confirmed"), anyString(), anyMap());
    }

    @Test
    void notifyBoqApprovalAction_changeRequestedWithMessage_appendsNote() {
        CustomerUser customer = userWithToken(3L, null); // no token → no push
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(50L);
        when(project.getName()).thenReturn("Villa A");

        service.notifyBoqApprovalAction(customer, project, "CHANGE_REQUESTED", "Please revise tiling");

        ArgumentCaptor<CustomerNotification> captor = ArgumentCaptor.forClass(CustomerNotification.class);
        verify(notificationRepository).save(captor.capture());
        CustomerNotification n = captor.getValue();
        assertThat(n.getNotificationType()).isEqualTo("BOQ_CHANGE_REQUESTED");
        assertThat(n.getTitle()).isEqualTo("Change Request Submitted");
        assertThat(n.getBody()).contains("Please revise tiling");
        verifyNoInteractions(pushNotificationService); // token blank → skipped
    }

    @Test
    void notifyBoqApprovalAction_exceptionSwallowed() {
        Project project = mock(Project.class);
        when(project.getName()).thenThrow(new RuntimeException("lazy proxy gone"));
        assertThatCode(() -> service.notifyBoqApprovalAction(userWithToken(3L, "t"), project, "APPROVED", null))
                .doesNotThrowAnyException();
    }

    // ── saveAndPush "no token" branch via a trigger ───────────────────────────

    @Test
    void saveAndPush_blankToken_persistsButSkipsPush() {
        CustomerUser user = userWithToken(3L, "  ");
        when(customerUserRepository.findById(3L)).thenReturn(Optional.of(user));

        service.notifyQueryReplied(3L, 50L, 1L, "t");

        verify(notificationRepository).save(any(CustomerNotification.class));
        verify(pushNotificationService, never()).sendToToken(any(), any(), any(), any());
    }
}
