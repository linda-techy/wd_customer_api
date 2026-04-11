package com.wd.custapi.repository;

import com.wd.custapi.model.BoqApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoqApprovalRepository extends JpaRepository<BoqApproval, Long> {

    /** Returns the most recent approval record for a given project. */
    Optional<BoqApproval> findTopByProjectIdOrderByCreatedAtDesc(Long projectId);
}
