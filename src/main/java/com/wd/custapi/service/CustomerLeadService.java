package com.wd.custapi.service;

import com.wd.custapi.dto.NewEnquiryRequest;
import com.wd.custapi.model.CustomerLead;
import com.wd.custapi.repository.CustomerLeadRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerLeadService {

    @Autowired
    private CustomerLeadRepository leadRepository;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<CustomerLead> getMyLeads(String email) {
        return leadRepository.findByEmailOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public List<CustomerLead> getMyReferrals(String email) {
        return leadRepository.findByReferredByEmailOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerLead> getMyLeadById(Long id, String email) {
        return leadRepository.findByIdAndEmail(id, email);
    }

    @Transactional
    public Long submitEnquiry(String email, NewEnquiryRequest request) {
        var user = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Insert via JDBC since CustomerLead is read-only (insertable=false).
        // The leads PK column is lead_id (auto-generated); RETURNING lead_id.
        return jdbc.queryForObject(
            "INSERT INTO leads (name, email, phone, project_type, state, district, location, "
          + "budget, project_sqft_area, requirements, lead_source, lead_status, priority, "
          + "customer_user_id, created_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, "
          + "CASE WHEN ? IS NOT NULL AND ? != '' THEN CAST(? AS NUMERIC) ELSE NULL END, "
          + "CASE WHEN ? IS NOT NULL AND ? != '' THEN CAST(? AS NUMERIC) ELSE NULL END, "
          + "?, 'CUSTOMER_APP', 'new_inquiry', 'HIGH', ?, now()) "
          + "RETURNING lead_id",
            Long.class,
            user.getFirstName() != null ? user.getFirstName() : user.getEmail(),
            user.getEmail(),
            user.getPhone() != null ? user.getPhone() : "",
            request.projectType(),
            request.state(),
            request.district(),
            request.location(),
            request.budget(), request.budget(), request.budget(),
            request.area(), request.area(), request.area(),
            request.requirements() != null ? request.requirements() : request.message(),
            user.getId()
        );
    }
}
