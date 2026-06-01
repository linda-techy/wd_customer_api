package com.wd.custapi.controller;

import com.wd.custapi.dto.NewEnquiryRequest;
import com.wd.custapi.model.CustomerLead;
import com.wd.custapi.service.CustomerLeadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Direct-invocation Mockito unit tests for {@link CustomerLeadController}.
 *
 * <p>The controller resolves the caller email from the static
 * {@link SecurityContextHolder}; each test seeds it in {@code setUp} and clears
 * it in {@code tearDown}. The single collaborator ({@link CustomerLeadService})
 * is mocked. No Spring context / MockMvc / DB is involved.
 */
@ExtendWith(MockitoExtension.class)
class CustomerLeadControllerTest {

    private static final String EMAIL = "customer@example.com";

    @Mock private CustomerLeadService leadService;

    @InjectMocks private CustomerLeadController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, "n/a", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- helpers ----

    private CustomerLead leadMock() {
        CustomerLead lead = mock(CustomerLead.class);
        lenient().when(lead.getId()).thenReturn(11L);
        lenient().when(lead.getName()).thenReturn("Asha");
        lenient().when(lead.getEmail()).thenReturn(EMAIL);
        lenient().when(lead.getPhone()).thenReturn("+919876543210");
        lenient().when(lead.getProjectType()).thenReturn("NEW_BUILD");
        lenient().when(lead.getBudget()).thenReturn(new BigDecimal("2500000"));
        lenient().when(lead.getProjectSqftArea()).thenReturn(new BigDecimal("1800"));
        lenient().when(lead.getLocation()).thenReturn("Kakkanad");
        lenient().when(lead.getDistrict()).thenReturn("Ernakulam");
        lenient().when(lead.getState()).thenReturn("Kerala");
        lenient().when(lead.getCustomerFriendlyStatus()).thenReturn("Enquiry Received");
        lenient().when(lead.getNextFollowUp()).thenReturn(LocalDateTime.of(2026, 6, 10, 9, 0));
        lenient().when(lead.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 6, 1, 8, 0));
        return lead;
    }

    // ---- GET /my ----

    @Test
    void getMyLeads_returnsCustomerViewList() {
        CustomerLead lead = leadMock();
        when(leadService.getMyLeads(EMAIL)).thenReturn(List.of(lead));

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyLeads();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
        Map<String, Object> view = response.getBody().get(0);
        // Internal pipeline fields must never be exposed
        assertThat(view)
                .containsEntry("id", 11L)
                .containsEntry("name", "Asha")
                .containsEntry("status", "Enquiry Received")
                .containsEntry("budget", "2500000")
                .containsEntry("area", "1800")
                .doesNotContainKey("leadStatus")
                .doesNotContainKey("leadSource");
    }

    @Test
    void getMyLeads_emptyList_returnsEmptyOk() {
        when(leadService.getMyLeads(EMAIL)).thenReturn(List.of());

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyLeads();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getMyLeads_nullFields_mapToEmptyStrings() {
        CustomerLead lead = mock(CustomerLead.class);
        when(lead.getId()).thenReturn(12L);
        // all string/number getters return null
        when(lead.getCustomerFriendlyStatus()).thenReturn("Processing");
        when(leadService.getMyLeads(EMAIL)).thenReturn(List.of(lead));

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyLeads();

        Map<String, Object> view = response.getBody().get(0);
        assertThat(view)
                .containsEntry("name", "")
                .containsEntry("budget", "")
                .containsEntry("area", "")
                .containsEntry("nextFollowUp", "")
                .containsEntry("createdAt", "")
                .containsEntry("status", "Processing");
    }

    // ---- GET /my-referrals ----

    @Test
    void getMyReferrals_masksPhoneAndUsesFriendKeys() {
        CustomerLead lead = leadMock();
        when(leadService.getMyReferrals(EMAIL)).thenReturn(List.of(lead));

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyReferrals();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> view = response.getBody().get(0);
        assertThat(view)
                .containsEntry("friendName", "Asha")
                .containsKey("friendPhone");
        // referral view only exposes a masked phone — last 3 digits visible
        String masked = (String) view.get("friendPhone");
        assertThat(masked).endsWith("210").contains("x");
        // referral view does NOT carry budget / location etc.
        assertThat(view)
                .containsEntry("status", "Enquiry Received")
                .doesNotContainKey("budget")
                .doesNotContainKey("email");
    }

    @Test
    void getMyReferrals_blankPhone_masksToEmptyString() {
        CustomerLead lead = mock(CustomerLead.class);
        when(lead.getId()).thenReturn(13L);
        when(lead.getName()).thenReturn("Ravi");
        when(lead.getPhone()).thenReturn("");
        when(lead.getCustomerFriendlyStatus()).thenReturn("Processing");
        when(leadService.getMyReferrals(EMAIL)).thenReturn(List.of(lead));

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyReferrals();

        assertThat(response.getBody().get(0)).containsEntry("friendPhone", "");
    }

    @Test
    void getMyReferrals_shortPhone_masksToXxx() {
        CustomerLead lead = mock(CustomerLead.class);
        when(lead.getId()).thenReturn(14L);
        when(lead.getName()).thenReturn("Sam");
        when(lead.getPhone()).thenReturn("12");
        when(lead.getCustomerFriendlyStatus()).thenReturn("Processing");
        when(leadService.getMyReferrals(EMAIL)).thenReturn(List.of(lead));

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyReferrals();

        assertThat(response.getBody().get(0)).containsEntry("friendPhone", "xxx");
    }

    @Test
    void getMyReferrals_localPhoneNoCountryCode_masksTrailingDigits() {
        CustomerLead lead = mock(CustomerLead.class);
        when(lead.getId()).thenReturn(15L);
        when(lead.getName()).thenReturn("Nila");
        when(lead.getPhone()).thenReturn("9876543210");
        when(lead.getCustomerFriendlyStatus()).thenReturn("Processing");
        when(leadService.getMyReferrals(EMAIL)).thenReturn(List.of(lead));

        ResponseEntity<List<Map<String, Object>>> response = controller.getMyReferrals();

        String masked = (String) response.getBody().get(0).get("friendPhone");
        assertThat(masked).isEqualTo("xxxxxxx210");
    }

    // ---- GET /my/{id} ----

    @Test
    void getMyLeadById_found_returnsCustomerView() {
        CustomerLead lead = leadMock();
        when(leadService.getMyLeadById(11L, EMAIL)).thenReturn(Optional.of(lead));

        ResponseEntity<Map<String, Object>> response = controller.getMyLeadById(11L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("id", 11L);
        assertThat(response.getBody()).containsEntry("location", "Kakkanad");
    }

    @Test
    void getMyLeadById_notFound_returns404() {
        when(leadService.getMyLeadById(99L, EMAIL)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getMyLeadById(99L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }

    // ---- POST /enquiry ----

    @Test
    void submitEnquiry_delegatesToServiceAndReturnsLeadId() {
        NewEnquiryRequest request = new NewEnquiryRequest(
                "NEW_BUILD", "Kerala", "Ernakulam", "Kakkanad",
                "2500000", "1800", "G+1 villa", null);
        when(leadService.submitEnquiry(EMAIL, request)).thenReturn(777L);

        ResponseEntity<Map<String, Object>> response = controller.submitEnquiry(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("success", true);
        assertThat(response.getBody()).containsEntry("leadId", 777L);
        assertThat(response.getBody()).containsEntry("message", "Enquiry submitted successfully");
    }
}
