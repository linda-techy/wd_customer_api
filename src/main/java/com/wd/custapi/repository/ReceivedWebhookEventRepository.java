package com.wd.custapi.repository;

import com.wd.custapi.model.ReceivedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceivedWebhookEventRepository extends JpaRepository<ReceivedWebhookEvent, Long> {

    List<ReceivedWebhookEvent> findByStatus(String status);
}
