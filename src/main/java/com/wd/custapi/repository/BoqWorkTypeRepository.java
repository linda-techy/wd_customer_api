package com.wd.custapi.repository;

import com.wd.custapi.model.BoqWorkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoqWorkTypeRepository extends JpaRepository<BoqWorkType, Long> {
    
    Optional<BoqWorkType> findByName(String name);
    
    List<BoqWorkType> findAllByOrderByDisplayOrderAsc();
}

