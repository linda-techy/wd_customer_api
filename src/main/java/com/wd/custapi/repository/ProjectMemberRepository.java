package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @Query("""
            SELECT pm FROM ProjectMember pm
             WHERE pm.projectId = :projectId
               AND pm.roleInProject IN :roles
               AND pm.portalUserId IS NOT NULL
            """)
    List<ProjectMember> findVisibleStaffByProject(
            @Param("projectId") Long projectId,
            @Param("roles") List<String> roles);
}
