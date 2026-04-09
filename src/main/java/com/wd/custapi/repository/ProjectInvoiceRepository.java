package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectInvoiceRepository extends JpaRepository<ProjectInvoice, Long> {

    /**
     * Paginated invoice list for a project — newest first.
     * Excludes DRAFT invoices (customers see ISSUED, PAID, CANCELLED only).
     */
    @Query("SELECT i FROM ProjectInvoice i " +
           "WHERE i.project.id = :projectId " +
           "AND i.status <> 'DRAFT' " +
           "ORDER BY i.invoiceDate DESC")
    Page<ProjectInvoice> findByProjectIdExcludingDraft(
            @Param("projectId") Long projectId, Pageable pageable);
}
