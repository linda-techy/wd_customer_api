package com.wd.custapi.repository;

import com.wd.custapi.model.PortfolioItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    Page<PortfolioItem> findByPublishedTrueOrderByCompletionDateDesc(Pageable pageable);

    Page<PortfolioItem> findByPublishedTrueAndProjectTypeIgnoreCaseOrderByCompletionDateDesc(String projectType, Pageable pageable);

    PortfolioItem findBySlugAndPublishedTrue(String slug);
}
