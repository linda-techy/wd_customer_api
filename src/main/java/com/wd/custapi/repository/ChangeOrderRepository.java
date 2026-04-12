package com.wd.custapi.repository;

import com.wd.custapi.model.ChangeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChangeOrderRepository extends JpaRepository<ChangeOrder, Long> {

    // @Where on ChangeOrder entity automatically appends "deleted_at IS NULL" to all queries
    List<ChangeOrder> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ChangeOrder> findByProjectIdAndStatus(Long projectId, String status);

    Optional<ChangeOrder> findByIdAndProjectId(Long id, Long projectId);
}
