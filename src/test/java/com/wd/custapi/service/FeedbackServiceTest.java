package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.FeedbackFormDto;
import com.wd.custapi.dto.ProjectModuleDtos.FeedbackFormRequest;
import com.wd.custapi.dto.ProjectModuleDtos.FeedbackResponseDto;
import com.wd.custapi.dto.ProjectModuleDtos.FeedbackResponseRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    // ─── Test data builders ─────────────────────────────────────────────────────

    private static final Long PROJECT_ID = 5L;

    private CustomerUser user(Long id, String first, String last) {
        CustomerUser u = new CustomerUser();
        u.setId(id);
        u.setFirstName(first);
        u.setLastName(last);
        return u;
    }

    private Project project(Long id) {
        Project p = new Project();
        p.setId(id);
        return p;
    }

    private FeedbackForm form(Long id, String title, Project project, CustomerUser createdBy) {
        FeedbackForm f = new FeedbackForm();
        f.setId(id);
        f.setTitle(title);
        f.setProject(project);
        f.setCreatedBy(createdBy);
        f.setIsActive(true);
        return f;
    }

    // ─── createForm ─────────────────────────────────────────────────────────────

    @Test
    void createForm_savesFormAndReturnsDto_whenProjectAndUserExist() {
        Project project = project(PROJECT_ID);
        CustomerUser creator = user(USER_ID, "Dave", "Lee");
        FeedbackFormRequest request = new FeedbackFormRequest("Title A", "Desc A", "SURVEY");

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(creator));
        // save returns the same form with createdBy/project intact (id set)
        when(feedbackFormRepository.save(any(FeedbackForm.class)))
                .thenAnswer(inv -> {
                    FeedbackForm f = inv.getArgument(0);
                    f.setId(100L);
                    return f;
                });

        FeedbackFormDto dto = feedbackService.createForm(PROJECT_ID, request, USER_ID);

        ArgumentCaptor<FeedbackForm> captor = ArgumentCaptor.forClass(FeedbackForm.class);
        verify(feedbackFormRepository).save(captor.capture());
        FeedbackForm saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Title A");
        assertThat(saved.getDescription()).isEqualTo("Desc A");
        assertThat(saved.getFormType()).isEqualTo("SURVEY");
        assertThat(saved.getProject()).isSameAs(project);
        assertThat(saved.getCreatedBy()).isSameAs(creator);

        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.projectId()).isEqualTo(PROJECT_ID);
        assertThat(dto.title()).isEqualTo("Title A");
        assertThat(dto.createdById()).isEqualTo(USER_ID);
        assertThat(dto.createdByName()).isEqualTo("Dave Lee");
        // createForm passes null isCompleted to toFormDto
        assertThat(dto.isCompleted()).isNull();
    }

    @Test
    void createForm_throws_whenProjectNotFound() {
        FeedbackFormRequest request = new FeedbackFormRequest("T", "D", "SURVEY");
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.createForm(PROJECT_ID, request, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Project not found");

        verify(feedbackFormRepository, never()).save(any());
    }

    @Test
    void createForm_throws_whenUserNotFound() {
        FeedbackFormRequest request = new FeedbackFormRequest("T", "D", "SURVEY");
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(PROJECT_ID)));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.createForm(PROJECT_ID, request, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(feedbackFormRepository, never()).save(any());
    }

    // ─── submitResponse ─────────────────────────────────────────────────────────

    @Test
    void submitResponse_savesResponseAndLogsActivity_whenNoExistingResponse() {
        Project project = project(PROJECT_ID);
        CustomerUser creator = user(7L, "Form", "Owner");
        CustomerUser submitter = user(USER_ID, "Eve", "Ng");
        FeedbackForm form = form(FORM_ID, "Survey X", project, creator);
        FeedbackResponseRequest request =
                new FeedbackResponseRequest(5, "Excellent", Map.of("k", "v"));

        when(feedbackFormRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(submitter));
        when(feedbackResponseRepository.findByFormIdAndCustomerId(FORM_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(feedbackResponseRepository.save(any(FeedbackResponse.class)))
                .thenAnswer(inv -> {
                    FeedbackResponse r = inv.getArgument(0);
                    r.setId(500L);
                    return r;
                });

        FeedbackResponseDto dto = feedbackService.submitResponse(FORM_ID, request, USER_ID);

        ArgumentCaptor<FeedbackResponse> captor = ArgumentCaptor.forClass(FeedbackResponse.class);
        verify(feedbackResponseRepository).save(captor.capture());
        FeedbackResponse saved = captor.getValue();
        assertThat(saved.getForm()).isSameAs(form);
        assertThat(saved.getProject()).isSameAs(project);
        assertThat(saved.getCustomer()).isSameAs(submitter);
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getComments()).isEqualTo("Excellent");
        assertThat(saved.getResponseData()).containsEntry("k", "v");

        // Activity logged with the FEEDBACK_SUBMITTED type and response id
        verify(activityFeedService).createActivity(
                PROJECT_ID, "FEEDBACK_SUBMITTED",
                "Feedback submitted: Survey X", 500L, USER_ID);

        assertThat(dto.id()).isEqualTo(500L);
        assertThat(dto.formId()).isEqualTo(FORM_ID);
        assertThat(dto.customerName()).isEqualTo("Eve Ng");
        assertThat(dto.rating()).isEqualTo(5);
    }

    @Test
    void submitResponse_throws_whenFormNotFound() {
        FeedbackResponseRequest request = new FeedbackResponseRequest(4, "ok", null);
        when(feedbackFormRepository.findById(FORM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.submitResponse(FORM_ID, request, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Feedback form not found");

        verify(feedbackResponseRepository, never()).save(any());
        verify(activityFeedService, never()).createActivity(anyLong(), anyString(), anyString(), anyLong(), anyLong());
    }

    @Test
    void submitResponse_throws_whenUserNotFound() {
        FeedbackForm form = form(FORM_ID, "Survey", project(PROJECT_ID), user(7L, "F", "O"));
        FeedbackResponseRequest request = new FeedbackResponseRequest(4, "ok", null);
        when(feedbackFormRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.submitResponse(FORM_ID, request, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(feedbackResponseRepository, never()).save(any());
    }

    @Test
    void submitResponse_throws_whenCustomerAlreadySubmitted() {
        Project project = project(PROJECT_ID);
        CustomerUser submitter = user(USER_ID, "Eve", "Ng");
        FeedbackForm form = form(FORM_ID, "Survey", project, user(7L, "F", "O"));
        FeedbackResponseRequest request = new FeedbackResponseRequest(4, "ok", null);

        FeedbackResponse existing = new FeedbackResponse();
        existing.setId(1L);

        when(feedbackFormRepository.findById(FORM_ID)).thenReturn(Optional.of(form));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(submitter));
        when(feedbackResponseRepository.findByFormIdAndCustomerId(FORM_ID, USER_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> feedbackService.submitResponse(FORM_ID, request, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You have already submitted feedback for this form");

        // No new response saved and no activity logged on the duplicate path
        verify(feedbackResponseRepository, never()).save(any());
        verify(activityFeedService, never()).createActivity(anyLong(), anyString(), anyString(), anyLong(), anyLong());
    }

    // ─── getProjectForms ────────────────────────────────────────────────────────

    @Test
    void getProjectForms_marksCompletedPerForm_basedOnCustomerResponse() {
        Project project = project(PROJECT_ID);
        CustomerUser creator = user(7L, "Form", "Owner");
        FeedbackForm formCompleted = form(1L, "Done Form", project, creator);
        FeedbackForm formPending = form(2L, "Pending Form", project, creator);

        when(feedbackFormRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID))
                .thenReturn(List.of(formCompleted, formPending));
        // Customer has responded to form 1 but not form 2
        when(feedbackResponseRepository.findByFormIdAndCustomerId(1L, USER_ID))
                .thenReturn(Optional.of(new FeedbackResponse()));
        when(feedbackResponseRepository.findByFormIdAndCustomerId(2L, USER_ID))
                .thenReturn(Optional.empty());

        List<FeedbackFormDto> result = feedbackService.getProjectForms(PROJECT_ID, USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).isCompleted()).isTrue();
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).isCompleted()).isFalse();
    }

    @Test
    void getProjectForms_returnsEmptyList_whenNoActiveForms() {
        when(feedbackFormRepository.findByProjectIdAndIsActiveTrue(PROJECT_ID))
                .thenReturn(List.of());

        List<FeedbackFormDto> result = feedbackService.getProjectForms(PROJECT_ID, USER_ID);

        assertThat(result).isEmpty();
        verify(feedbackResponseRepository, never()).findByFormIdAndCustomerId(anyLong(), anyLong());
    }

    // ─── getFormResponses (admin — ALL customers) ───────────────────────────────

    @Test
    void getFormResponses_returnsAllResponsesMapped_usingRawFindByFormId() {
        Project project = project(PROJECT_ID);
        FeedbackForm form = form(FORM_ID, "Survey", project, user(7L, "F", "O"));

        FeedbackResponse r1 = new FeedbackResponse();
        r1.setId(1L);
        r1.setForm(form);
        r1.setProject(project);
        r1.setCustomer(user(11L, "Cust", "One"));
        r1.setRating(5);

        FeedbackResponse r2 = new FeedbackResponse();
        r2.setId(2L);
        r2.setForm(form);
        r2.setProject(project);
        r2.setCustomer(user(12L, "Cust", "Two"));
        r2.setRating(3);

        when(feedbackResponseRepository.findByFormId(FORM_ID)).thenReturn(List.of(r1, r2));

        List<FeedbackResponseDto> result = feedbackService.getFormResponses(FORM_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(FeedbackResponseDto::id).containsExactly(1L, 2L);
        assertThat(result).extracting(FeedbackResponseDto::customerName)
                .containsExactly("Cust One", "Cust Two");
        verify(feedbackResponseRepository).findByFormId(FORM_ID);
    }

    @Test
    void getFormResponses_returnsEmptyList_whenNoResponses() {
        when(feedbackResponseRepository.findByFormId(FORM_ID)).thenReturn(List.of());

        List<FeedbackResponseDto> result = feedbackService.getFormResponses(FORM_ID);

        assertThat(result).isEmpty();
    }
}
