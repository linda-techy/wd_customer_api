package com.wd.custapi.repository;

import com.wd.custapi.model.DeductionRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeductionRegisterRepository extends JpaRepository<DeductionRegister, Long> {

    List<DeductionRegister> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
