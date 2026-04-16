package com.wd.custapi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Proxies public referral submissions to the Portal API.
 * Keeps the customer app's API surface on a single base URL
 * instead of exposing the portal API URL in mobile config.
 */
@RestController
@RequestMapping("/api/leads")
public class ReferralProxyController {

    @Value("${portal.api.url:http://localhost:8080}")
    private String portalApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/referral")
    public ResponseEntity<Map<String, Object>> proxyReferral(@RequestBody Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

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
}
