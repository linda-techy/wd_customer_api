package com.wd.custapi.repository;

import com.wd.custapi.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

        // Get all projects for a customer (ordered by latest first)
        @Query(value = "SELECT DISTINCT p.* FROM customer_projects p " +
                        "LEFT JOIN project_members cpm ON p.id = cpm.project_id " +
                        "LEFT JOIN customer_users c_mem ON cpm.customer_user_id = c_mem.id " +
                        "LEFT JOIN customer_users c_owner ON p.customer_id = c_owner.id " +
                        "WHERE c_mem.email = :email OR c_owner.email = :email " +
                        "ORDER BY p.id DESC", nativeQuery = true)
        List<Project> findAllByCustomerEmail(@Param("email") String email);

        // Get recent N projects for a customer
        @Query(value = "SELECT DISTINCT p.* FROM customer_projects p " +
                        "LEFT JOIN project_members cpm ON p.id = cpm.project_id " +
                        "LEFT JOIN customer_users c_mem ON cpm.customer_user_id = c_mem.id " +
                        "LEFT JOIN customer_users c_owner ON p.customer_id = c_owner.id " +
                        "WHERE (c_mem.email = :email OR c_owner.email = :email) " +
                        "ORDER BY p.id DESC LIMIT :limit", nativeQuery = true)
        List<Project> findRecentByCustomerEmail(@Param("email") String email, @Param("limit") int limit);

        // Search projects by name, code, or location
        @Query(value = "SELECT DISTINCT p.* FROM customer_projects p " +
                        "LEFT JOIN project_members cpm ON p.id = cpm.project_id " +
                        "LEFT JOIN customer_users c_mem ON cpm.customer_user_id = c_mem.id " +
                        "LEFT JOIN customer_users c_owner ON p.customer_id = c_owner.id " +
                        "WHERE (c_mem.email = :email OR c_owner.email = :email) " +
                        "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                        "OR LOWER(p.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                        "OR LOWER(p.location) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
                        "ORDER BY p.id DESC", nativeQuery = true)
        List<Project> searchByCustomerEmailAndTerm(@Param("email") String email,
                        @Param("searchTerm") String searchTerm);

        @Query(value = "SELECT COUNT(p.id) FROM customer_projects p " +
                        "INNER JOIN project_members cpm ON p.id = cpm.project_id " +
                        "WHERE cpm.customer_user_id = :customerId", nativeQuery = true)
        long countByCustomerId(@Param("customerId") Long customerId);

        // Get specific project by ID for a customer (ensures user can only access their
        // projects)
        @Query(value = "SELECT DISTINCT p.* FROM customer_projects p " +
                        "LEFT JOIN project_members cpm ON p.id = cpm.project_id " +
                        "LEFT JOIN customer_users c_mem ON cpm.customer_user_id = c_mem.id " +
                        "LEFT JOIN customer_users c_owner ON p.customer_id = c_owner.id " +
                        "WHERE p.id = :projectId AND (c_mem.email = :email OR c_owner.email = :email)", nativeQuery = true)
        Project findByIdAndCustomerEmail(@Param("projectId") Long projectId, @Param("email") String email);

        // Get specific project by Project UUID for a customer
        @Query(value = "SELECT DISTINCT p.* FROM customer_projects p " +
                        "LEFT JOIN project_members cpm ON p.id = cpm.project_id " +
                        "LEFT JOIN customer_users c_mem ON cpm.customer_user_id = c_mem.id " +
                        "LEFT JOIN customer_users c_owner ON p.customer_id = c_owner.id " +
                        "WHERE p.project_uuid = :projectUuid AND (c_mem.email = :email OR c_owner.email = :email)", nativeQuery = true)
        Project findByProjectUuidAndCustomerEmail(@Param("projectUuid") java.util.UUID projectUuid,
                        @Param("email") String email);

        // --- Admin: see all projects (no project_members filter) ---
        @Query(value = "SELECT p.* FROM customer_projects p ORDER BY p.id DESC", nativeQuery = true)
        List<Project> findAllForAdmin();

        @Query(value = "SELECT p.* FROM customer_projects p ORDER BY p.id DESC LIMIT :limit", nativeQuery = true)
        List<Project> findRecentForAdmin(@Param("limit") int limit);

        @Query(value = "SELECT p.* FROM customer_projects p " +
                        "WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                        "OR LOWER(p.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                        "OR LOWER(p.location) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
                        "ORDER BY p.id DESC", nativeQuery = true)
        List<Project> searchForAdmin(@Param("searchTerm") String searchTerm);

        @Query(value = "SELECT p.* FROM customer_projects p WHERE p.project_uuid = :projectUuid", nativeQuery = true)
        Project findByProjectUuid(@Param("projectUuid") java.util.UUID projectUuid);
}
