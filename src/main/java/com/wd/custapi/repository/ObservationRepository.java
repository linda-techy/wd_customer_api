package com.wd.custapi.repository;

import com.wd.custapi.model.Observation;
import com.wd.custapi.model.Observation.ObservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, Long> {

    List<Observation> findByProjectIdAndStatus(Long projectId, ObservationStatus status);

    List<Observation> findByProjectIdOrderByReportedDateDesc(Long projectId);

    List<Observation> findByProjectIdAndStatusOrderByPriorityDescReportedDateDesc(
            Long projectId, ObservationStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT o FROM Observation o WHERE o.project.id = :projectId AND o.status IN :statuses ORDER BY o.priority DESC, o.reportedDate DESC")
    List<Observation> findByProjectIdAndStatusInOrderByPriorityDescReportedDateDesc(
            @org.springframework.data.repository.query.Param("projectId") Long projectId,
            @org.springframework.data.repository.query.Param("statuses") List<ObservationStatus> statuses);
}
