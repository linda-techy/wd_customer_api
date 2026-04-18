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

    @Query(value = "SELECT nextval('support_ticket_seq')", nativeQuery = true)
    Long getNextTicketSequence();
}
