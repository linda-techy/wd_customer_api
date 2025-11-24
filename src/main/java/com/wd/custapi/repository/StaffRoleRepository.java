package com.wd.custapi.repository;

import com.wd.custapi.model.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRoleRepository extends JpaRepository<StaffRole, Long> {
    
    Optional<StaffRole> findByName(String name);
}

