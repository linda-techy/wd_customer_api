package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectQueryRepository extends JpaRepository<ProjectQuery, Long> {
    
    List<ProjectQuery> findByProjectIdAndStatus(Long projectId, ProjectQuery.QueryStatus status);
    
    List<ProjectQuery> findByProjectIdOrderByRaisedDateDesc(Long projectId);
    
    List<ProjectQuery> findByProjectIdAndStatusOrderByPriorityDescRaisedDateDesc(
        Long projectId, ProjectQuery.QueryStatus status);
    
    List<ProjectQuery> findByProjectIdAndCategory(Long projectId, String category);
}

