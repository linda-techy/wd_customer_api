package com.wd.custapi.repository;

import com.wd.custapi.model.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Page<BlogPost> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    Page<BlogPost> findByPublishedTrueAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(String title, Pageable pageable);

    BlogPost findBySlugAndPublishedTrue(String slug);
}
