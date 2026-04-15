package com.wd.custapi.repository;

import com.wd.custapi.model.FinalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinalAccountRepository extends JpaRepository<FinalAccount, Long> {

    Optional<FinalAccount> findByProjectId(Long projectId);
}
