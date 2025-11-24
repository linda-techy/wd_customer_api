package com.wd.custapi.repository;

import com.wd.custapi.model.CustomerUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerUserRepository extends JpaRepository<CustomerUser, Long> {
    Optional<CustomerUser> findByEmail(String email);
    
    @Query("SELECT u FROM CustomerUser u WHERE u.role.id = :roleId AND u.enabled = true")
    List<CustomerUser> findByRoleId(@Param("roleId") Long roleId);
}

