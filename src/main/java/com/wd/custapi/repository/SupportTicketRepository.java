package com.wd.custapi.repository;

import com.wd.custapi.model.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Page<SupportTicket> findByCustomerUser_IdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Page<SupportTicket> findByCustomerUser_IdAndStatusOrderByUpdatedAtDesc(Long userId, String status, Pageable pageable);

    // Portable "next id" that works without a Postgres sequence object —
    // critical for test environments using Hibernate create-drop where the
    // sequence isn't auto-created.
    @Query(value = "SELECT COALESCE(MAX(id), 0) + 1 FROM support_tickets", nativeQuery = true)
    Long getNextTicketSequence();
}
