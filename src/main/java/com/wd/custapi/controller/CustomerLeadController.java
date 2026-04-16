package com.wd.custapi.controller;

import com.wd.custapi.dto.NewEnquiryRequest;
import com.wd.custapi.model.CustomerLead;
import com.wd.custapi.service.CustomerLeadService;
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

    @GetMapping("/my/{id}")
    public ResponseEntity<Map<String, Object>> getMyLeadById(@PathVariable Long id) {
        String email = currentEmail();
        return leadService.getMyLeadById(id, email)
                .map(lead -> ResponseEntity.ok(toCustomerView(lead)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/enquiry")
    public ResponseEntity<Map<String, Object>> submitEnquiry(@RequestBody NewEnquiryRequest request) {
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
        map.put("internalStatus", lead.getLeadStatus() != null ? lead.getLeadStatus() : "");
        map.put("source", lead.getLeadSource() != null ? lead.getLeadSource() : "");
        map.put("nextFollowUp", lead.getNextFollowUp() != null ? lead.getNextFollowUp().toString() : "");
        map.put("createdAt", lead.getCreatedAt() != null ? lead.getCreatedAt().toString() : "");
        return map;
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
