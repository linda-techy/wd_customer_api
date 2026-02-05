package com.wd.custapi.repository;

import com.wd.custapi.model.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, Long> {
    
    List<Observation> findByProjectIdAndStatus(Long projectId, Observation.ObservationStatus status);
    
    List<Observation> findByProjectIdOrderByReportedDateDesc(Long projectId);
    
    List<Observation> findByProjectIdAndStatusOrderByPriorityDescReportedDateDesc(
        Long projectId, Observation.ObservationStatus status);
    
    List<Observation> findByProjectIdAndStatusInOrderByPriorityDescReportedDateDesc(
        Long projectId, List<Observation.ObservationStatus> statuses);
}

