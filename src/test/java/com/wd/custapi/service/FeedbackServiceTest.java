package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.FeedbackResponseDto;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.FeedbackForm;
import com.wd.custapi.model.FeedbackResponse;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.FeedbackFormRepository;
import com.wd.custapi.repository.FeedbackResponseRepository;
import com.wd.custapi.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeedbackService.
 *
 * Critical regression guard: the customer-facing responses query MUST use the
 * customer-scoped repository method ({@code findByFormIdAndCustomerId}) and
 * NEVER the raw unfiltered {@code findByFormId} which leaks all customers'
 * feedback to any caller.
 *
 * Mirrors the "filtered-not-raw" verify pattern used in
 * CustomerDelayLogControllerTest.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock private FeedbackFormRepository feedbackFormRepository;
    @Mock private FeedbackResponseRepository feedbackResponseRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CustomerUserRepository userRepository;
    @Mock private ActivityFeedService activityFeedService;

    @InjectMocks
    private FeedbackService feedbackService;

    private static final Long FORM_ID = 10L;
    private static final Long USER_ID = 42L;

    // ─── getFormResponsesForCustomer — scoping guard ────────────────────────────

    @Test
    void getFormResponsesForCustomer_callsCustomerScopedQuery_notRawFindByFormId() {
        // Given – repository returns empty Optional (no response yet for this customer)
        when(feedbackResponseRepository.findByFormIdAndCustomerId(FORM_ID, USER_ID))
                .thenReturn(Optional.empty());

        // When
        feedbackService.getFormResponsesForCustomer(FORM_ID, USER_ID);

        // Then – must use the customer-scoped query
        verify(feedbackResponseRepository).findByFormIdAndCustomerId(FORM_ID, USER_ID);
        // And NEVER use the raw unfiltered query that returns ALL customers' responses
        verify(feedbackResponseRepository, never()).findByFormId(anyLong());
    }

    @Test
    void getFormResponsesForCustomer_returnsEmptyList_whenNoResponse() {
        when(feedbackResponseRepository.findByFormIdAndCustomerId(FORM_ID, USER_ID))
                .thenReturn(Optional.empty());

        List<FeedbackResponseDto> result = feedbackService.getFormResponsesForCustomer(FORM_ID, USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getFormResponsesForCustomer_returnsMappedDto_whenResponseExists() {
        // Build a minimal FeedbackResponse stub
        Project project = new Project();
        project.setId(5L);

        CustomerUser customer = new CustomerUser();
        customer.setId(USER_ID);
        customer.setFirstName("Alice");
        customer.setLastName("Smith");

        FeedbackForm form = new FeedbackForm();
        form.setId(FORM_ID);
        form.setTitle("Q1 Survey");
        form.setProject(project);
        form.setCreatedBy(customer);

        FeedbackResponse response = new FeedbackResponse();
        response.setId(99L);
        response.setForm(form);
        response.setProject(project);
        response.setCustomer(customer);
        response.setRating(5);
        response.setComments("Great job");
        response.setSubmittedAt(LocalDateTime.now());

        when(feedbackResponseRepository.findByFormIdAndCustomerId(FORM_ID, USER_ID))
                .thenReturn(Optional.of(response));

        List<FeedbackResponseDto> result = feedbackService.getFormResponsesForCustomer(FORM_ID, USER_ID);

        assertThat(result).hasSize(1);
        FeedbackResponseDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo(99L);
        assertThat(dto.customerId()).isEqualTo(USER_ID);
        assertThat(dto.customerName()).isEqualTo("Alice Smith");
        assertThat(dto.rating()).isEqualTo(5);
        assertThat(dto.comments()).isEqualTo("Great job");
    }

    // ─── admin reply fields are propagated to the DTO ───────────────────────────

    @Test
    void getFormResponsesForCustomer_adminReplyFieldsPopulatedInDto_whenAdminHasReplied() {
        Project project = new Project();
        project.setId(5L);

        CustomerUser customer = new CustomerUser();
        customer.setId(USER_ID);
        customer.setFirstName("Bob");
        customer.setLastName("Jones");

        FeedbackForm form = new FeedbackForm();
        form.setId(FORM_ID);
        form.setTitle("Post-Handover Survey");
        form.setProject(project);
        form.setCreatedBy(customer);

        LocalDateTime repliedAt = LocalDateTime.of(2026, 5, 26, 10, 0, 0);

        FeedbackResponse response = new FeedbackResponse();
        response.setId(77L);
        response.setForm(form);
        response.setProject(project);
        response.setCustomer(customer);
        response.setRating(4);
        response.setComments("Good work");
        response.setSubmittedAt(LocalDateTime.now().minusDays(1));
        // Admin has replied
        response.setAdminResponse("Thank you for your feedback!");
        response.setAdminRespondedAt(repliedAt);

        when(feedbackResponseRepository.findByFormIdAndCustomerId(FORM_ID, USER_ID))
                .thenReturn(Optional.of(response));

        List<FeedbackResponseDto> result = feedbackService.getFormResponsesForCustomer(FORM_ID, USER_ID);

        assertThat(result).hasSize(1);
        FeedbackResponseDto dto = result.get(0);
        assertThat(dto.adminResponse()).isEqualTo("Thank you for your feedback!");
        assertThat(dto.adminRespondedAt()).isEqualTo(repliedAt);
    }

    @Test
    void getFormResponsesForCustomer_adminReplyFieldsNull_whenNoReplyYet() {
        Project project = new Project();
        project.setId(5L);

        CustomerUser customer = new CustomerUser();
        customer.setId(USER_ID);
        customer.setFirstName("Carol");
        customer.setLastName("White");

        FeedbackForm form = new FeedbackForm();
        form.setId(FORM_ID);
        form.setTitle("Q2 Survey");
        form.setProject(project);
        form.setCreatedBy(customer);

        FeedbackResponse response = new FeedbackResponse();
        response.setId(88L);
        response.setForm(form);
        response.setProject(project);
        response.setCustomer(customer);
        response.setRating(3);
        response.setComments("OK");
        response.setSubmittedAt(LocalDateTime.now());
        // No admin reply set — fields remain null

        when(feedbackResponseRepository.findByFormIdAndCustomerId(FORM_ID, USER_ID))
                .thenReturn(Optional.of(response));

        List<FeedbackResponseDto> result = feedbackService.getFormResponsesForCustomer(FORM_ID, USER_ID);

        assertThat(result).hasSize(1);
        FeedbackResponseDto dto = result.get(0);
        assertThat(dto.adminResponse()).isNull();
        assertThat(dto.adminRespondedAt()).isNull();
    }

    // ─── Isolation: other customer's responses are NOT returned ─────────────────

    @Test
    void getFormResponsesForCustomer_onlyReturnsCallerResponse_neverOtherCustomers() {
        // Simulate: two responses exist in the DB but only USER_ID's is returned
        when(feedbackResponseRepository.findByFormIdAndCustomerId(FORM_ID, USER_ID))
                .thenReturn(Optional.empty());

        List<FeedbackResponseDto> result = feedbackService.getFormResponsesForCustomer(FORM_ID, USER_ID);

        // The caller gets an empty list — no other customers' data leaks through
        assertThat(result).isEmpty();
        // The raw query that would return ALL responses is never touched
        verify(feedbackResponseRepository, never()).findByFormId(FORM_ID);
    }
}
