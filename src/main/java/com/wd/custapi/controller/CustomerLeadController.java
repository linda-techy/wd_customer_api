package com.wd.custapi.controller;

import com.wd.custapi.dto.NewEnquiryRequest;
import com.wd.custapi.model.CustomerLead;
import com.wd.custapi.service.CustomerLeadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/leads")
@PreAuthorize("isAuthenticated()")
public class CustomerLeadController {

    @Autowired
    private CustomerLeadService leadService;

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyLeads() {
        String email = currentEmail();
        List<CustomerLead> leads = leadService.getMyLeads(email);
        return ResponseEntity.ok(leads.stream().map(this::toCustomerView).toList());
    }

    @GetMapping("/my-referrals")
    public ResponseEntity<List<Map<String, Object>>> getMyReferrals() {
        String email = currentEmail();
        List<CustomerLead> referrals = leadService.getMyReferrals(email);
        return ResponseEntity.ok(referrals.stream().map(this::toReferralView).toList());
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<Map<String, Object>> getMyLeadById(@PathVariable Long id) {
        String email = currentEmail();
        return leadService.getMyLeadById(id, email)
                .map(lead -> ResponseEntity.ok(toCustomerView(lead)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/enquiry")
    public ResponseEntity<Map<String, Object>> submitEnquiry(@Valid @RequestBody NewEnquiryRequest request) {
        String email = currentEmail();
        Long leadId = leadService.submitEnquiry(email, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Enquiry submitted successfully", "leadId", leadId));
    }

    private Map<String, Object> toCustomerView(CustomerLead lead) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", lead.getId());
        map.put("name", lead.getName() != null ? lead.getName() : "");
        map.put("email", lead.getEmail() != null ? lead.getEmail() : "");
        map.put("phone", lead.getPhone() != null ? lead.getPhone() : "");
        map.put("projectType", lead.getProjectType() != null ? lead.getProjectType() : "");
        map.put("budget", lead.getBudget() != null ? lead.getBudget().toString() : "");
        map.put("area", lead.getProjectSqftArea() != null ? lead.getProjectSqftArea().toString() : "");
        map.put("location", lead.getLocation() != null ? lead.getLocation() : "");
        map.put("district", lead.getDistrict() != null ? lead.getDistrict() : "");
        map.put("state", lead.getState() != null ? lead.getState() : "");
        map.put("status", lead.getCustomerFriendlyStatus());
        // Internal pipeline fields (leadStatus, leadSource) intentionally omitted — not customer-facing.
        map.put("nextFollowUp", lead.getNextFollowUp() != null ? lead.getNextFollowUp().toString() : "");
        map.put("createdAt", lead.getCreatedAt() != null ? lead.getCreatedAt().toString() : "");
        return map;
    }

    private Map<String, Object> toReferralView(CustomerLead lead) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", lead.getId());
        map.put("friendName", lead.getName() != null ? lead.getName() : "");
        map.put("friendPhone", maskPhone(lead.getPhone()));
        map.put("projectType", lead.getProjectType() != null ? lead.getProjectType() : "");
        map.put("status", lead.getCustomerFriendlyStatus());
        map.put("createdAt", lead.getCreatedAt() != null ? lead.getCreatedAt().toString() : "");
        return map;
    }

    /**
     * Mask a phone so the referrer sees only the last 3 digits.
     * Example: +919876543210 -> "+91xxxxxxx210", 9876543210 -> "xxxxxxx210".
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) return "xxx";
        String cc = phone.startsWith("+") ? phone.substring(0, Math.min(3, phone.indexOf(digits.charAt(0)) + 3)) : "";
        String last3 = digits.substring(digits.length() - 3);
        int hiddenLen = digits.length() - 3 - (cc.isEmpty() ? 0 : cc.replaceAll("\\D", "").length());
        if (hiddenLen < 0) hiddenLen = digits.length() - 3;
        return cc + "x".repeat(hiddenLen) + last3;
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
