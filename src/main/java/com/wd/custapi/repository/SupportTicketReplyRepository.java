package com.wd.custapi.repository;

import com.wd.custapi.model.SupportTicketReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketReplyRepository extends JpaRepository<SupportTicketReply, Long> {

    List<SupportTicketReply> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
