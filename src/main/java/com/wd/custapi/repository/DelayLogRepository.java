package com.wd.custapi.repository;

import com.wd.custapi.model.DelayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DelayLogRepository extends JpaRepository<DelayLog, Long> {
    List<DelayLog> findByProjectIdOrderByFromDateDesc(Long projectId);
}
