package com.wd.custapi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Proxies authenticated customer referral submissions to the Portal API.
 * Keeps the customer app's API surface on a single base URL
 * instead of exposing the portal API URL in mobile config.
 *
 * The authenticated caller's email is stamped into `yourEmail` server-side
 * so a customer cannot spoof a referral as another person.
 */
@RestController
@RequestMapping("/api/leads")
public class ReferralProxyController {

    @Value("${portal.api.url:http://localhost:8080}")
    private String portalApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/referral")
    public ResponseEntity<Map<String, Object>> proxyReferral(
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "Authentication required"));
        }
        try {
            Map<String, Object> safeBody = buildSafeBody(body, auth.getName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(safeBody, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>)
                restTemplate.exchange(
                    portalApiUrl + "/leads/referral",
                    HttpMethod.POST,
                    entity,
                    Map.class
                );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to submit referral"));
        }
    }

    private Map<String, Object> buildSafeBody(Map<String, Object> body, String authenticatedEmail) {
        Map<String, Object> out = new HashMap<>();
        String[] allowed = {
            "referralName", "referralEmail", "referralPhone",
            "yourName", "yourPhone",
            "projectType", "estimatedBudget", "location", "state", "district", "message"
        };
        if (body != null) {
            for (String k : allowed) {
                Object v = body.get(k);
                if (v != null) out.put(k, v);
            }
        }
        // Always overwrite referrer identity with authenticated email; no spoofing allowed.
        out.put("yourEmail", authenticatedEmail);
        return out;
    }
}
