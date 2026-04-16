package com.wd.custapi.repository;

import com.wd.custapi.model.CustomerLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerLeadRepository extends JpaRepository<CustomerLead, Long> {
    List<CustomerLead> findByEmailOrderByCreatedAtDesc(String email);
    Optional<CustomerLead> findByIdAndEmail(Long id, String email);
    List<CustomerLead> findByReferredByEmailOrderByCreatedAtDesc(String email);
}
