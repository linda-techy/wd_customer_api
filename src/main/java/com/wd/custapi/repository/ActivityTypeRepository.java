package com.wd.custapi.repository;

import com.wd.custapi.model.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityTypeRepository extends JpaRepository<ActivityType, Long> {
    
    Optional<ActivityType> findByName(String name);
}

