package com.wd.custapi.controller;

import com.wd.custapi.dto.ProjectModuleDtos.ApiResponse;
import com.wd.custapi.model.CustomerNotification;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.repository.CustomerNotificationRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Direct-invocation Mockito unit tests for {@link NotificationController}.
 *
 * <p>Both collaborators (notification + user repositories) and the
 * {@link Authentication} are mocked. Covers list/markRead/markAllRead happy
 * paths, the 403 ownership branch, 404 not-found, and the 500 generic-error
 * branches. No Spring / MockMvc / DB.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final String EMAIL = "customer@example.com";
    private static final Long USER_ID = 24L;

    @Mock private CustomerNotificationRepository notificationRepository;
    @Mock private CustomerUserRepository customerUserRepository;
    @Mock private Authentication auth;

    @InjectMocks private NotificationController controller;

    private CustomerUser user;

    @BeforeEach
    void setUp() {
        user = new CustomerUser();
        user.setId(USER_ID);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(customerUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private CustomerNotification notificationMock(boolean owned) {
        CustomerNotification n = mock(CustomerNotification.class);
        CustomerUser owner = new CustomerUser();
        owner.setId(owned ? USER_ID : 999L);
        lenient().when(n.getCustomerUser()).thenReturn(owner);
        return n;
    }

    // ---- GET /api/notifications ----

    @Test
    void getNotifications_success_returnsEnvelopeWithUnreadCount() {
        CustomerNotification n = mock(CustomerNotification.class);
        when(n.getId()).thenReturn(1L);
        when(n.getProjectId()).thenReturn(50L);
        when(n.getTitle()).thenReturn("Site report ready");
        when(n.getBody()).thenReturn("Your weekly report is available");
        when(n.getNotificationType()).thenReturn("SITE_REPORT");
        when(n.getReferenceId()).thenReturn(7L);
        when(n.isRead()).thenReturn(false);
        when(n.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 6, 1, 10, 0));

        Page<CustomerNotification> page = new PageImpl<>(List.of(n), PageRequest.of(0, 20), 1);
        when(notificationRepository.findByCustomerUser_IdOrderByCreatedAtDesc(eq_USER_ID(), any(Pageable.class)))
                .thenReturn(page);
        when(notificationRepository.countByCustomerUser_IdAndReadFalse(USER_ID)).thenReturn(3L);

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                controller.getNotifications(0, 20, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        ApiResponse<Map<String, Object>> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isTrue();
        assertThat(body.message()).isEqualTo("Notifications retrieved");
        Map<String, Object> data = body.data();
        assertThat(data)
                .containsEntry("totalElements", 1L)
                .containsEntry("totalPages", 1)
                .containsEntry("currentPage", 0)
                .containsEntry("unreadCount", 3L);
        assertThat((List<?>) data.get("notifications")).hasSize(1);
    }

    @Test
    void getNotifications_capsPageSizeAtFifty() {
        Page<CustomerNotification> page = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
        when(notificationRepository.findByCustomerUser_IdOrderByCreatedAtDesc(eq_USER_ID(), any(Pageable.class)))
                .thenReturn(page);
        when(notificationRepository.countByCustomerUser_IdAndReadFalse(USER_ID)).thenReturn(0L);

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                controller.getNotifications(0, 200, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        // Size capped at 50: verify the Pageable passed to the repo carries pageSize=50
        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByCustomerUser_IdOrderByCreatedAtDesc(eq_USER_ID(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void getNotifications_userNotFound_returns500() {
        when(customerUserRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                controller.getNotifications(0, 20, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Failed to retrieve notifications");
        assertThat(response.getBody().data()).isNull();
    }

    // ---- PUT /api/notifications/{id}/read ----

    @Test
    void markRead_success_marksAndSaves() {
        CustomerNotification n = notificationMock(true);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        ResponseEntity<ApiResponse<String>> response = controller.markRead(5L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isEqualTo("ok");
        verify(n).setRead(true);
        verify(notificationRepository).save(n);
    }

    @Test
    void markRead_notOwner_returns403AndDoesNotSave() {
        CustomerNotification n = notificationMock(false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        ResponseEntity<ApiResponse<String>> response = controller.markRead(5L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Access denied");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_notFound_returns404() {
        when(notificationRepository.findById(5L)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<String>> response = controller.markRead(5L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains("not found");
    }

    @Test
    void markRead_saveThrows_returns500() {
        CustomerNotification n = notificationMock(true);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(n)).thenThrow(new RuntimeException("db error"));

        ResponseEntity<ApiResponse<String>> response = controller.markRead(5L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Failed to mark notification as read");
    }

    // ---- PUT /api/notifications/read-all ----

    @Test
    void markAllRead_success_returnsCount() {
        when(notificationRepository.markAllReadByUserId(USER_ID)).thenReturn(4);

        ResponseEntity<ApiResponse<String>> response = controller.markAllRead(auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("4 notification(s) marked as read");
        assertThat(response.getBody().data()).isEqualTo("ok");
    }

    @Test
    void markAllRead_userNotFound_returns500() {
        when(customerUserRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<String>> response = controller.markAllRead(auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("Failed to mark notifications as read");
    }

    // matcher helper to keep the long literal readable
    private static Long eq_USER_ID() {
        return org.mockito.ArgumentMatchers.eq(USER_ID);
    }
}
