package com.wd.custapi.repository;

import com.wd.custapi.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Timeline bucket queries for the customer-facing Timeline endpoint.
 * All queries filter on customer_visible = TRUE so only portal-approved
 * tasks surface to the customer app.
 */
public interface CustomerTaskRepository extends JpaRepository<Task, Long> {

    /**
     * Week bucket: customer-visible, incomplete tasks whose date range overlaps
     * the current ISO week [weekStart, weekEnd].
     */
    @Query("""
            SELECT t FROM Task t
             WHERE t.project.id = :projectId
               AND t.customerVisible = TRUE
               AND t.status <> 'COMPLETED'
               AND t.startDate <= :weekEnd
               AND t.endDate >= :weekStart
            ORDER BY t.startDate ASC
            """)
    List<Task> findWeekBucket(@Param("projectId") Long projectId,
                              @Param("weekStart") LocalDate weekStart,
                              @Param("weekEnd") LocalDate weekEnd);

    /**
     * Upcoming bucket: customer-visible, incomplete tasks that start after the
     * current week.
     */
    @Query("""
            SELECT t FROM Task t
             WHERE t.project.id = :projectId
               AND t.customerVisible = TRUE
               AND t.status <> 'COMPLETED'
               AND t.startDate > :weekEnd
            ORDER BY t.startDate ASC
            """)
    List<Task> findUpcomingBucket(@Param("projectId") Long projectId,
                                  @Param("weekEnd") LocalDate weekEnd);

    /**
     * Completed bucket: customer-visible tasks that are done (either by status
     * or 100 % progress).
     */
    @Query("""
            SELECT t FROM Task t
             WHERE t.project.id = :projectId
               AND t.customerVisible = TRUE
               AND (t.status = 'COMPLETED' OR t.progressPercent = 100)
            ORDER BY t.endDate DESC
            """)
    List<Task> findCompletedBucket(@Param("projectId") Long projectId);
}
