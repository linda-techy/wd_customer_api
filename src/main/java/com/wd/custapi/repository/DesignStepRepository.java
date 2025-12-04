package com.wd.custapi.repository;

import com.wd.custapi.model.DesignStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesignStepRepository extends JpaRepository<DesignStep, Long> {
}
