package com.wd.custapi.service;

import com.wd.custapi.model.BlogPost;
import com.wd.custapi.model.PortfolioItem;
import com.wd.custapi.repository.BlogPostRepository;
import com.wd.custapi.repository.PortfolioItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ContentService {

    private static final Logger logger = LoggerFactory.getLogger(ContentService.class);

    private final BlogPostRepository blogPostRepository;
    private final PortfolioItemRepository portfolioItemRepository;

    public ContentService(BlogPostRepository blogPostRepository,
                          PortfolioItemRepository portfolioItemRepository) {
        this.blogPostRepository = blogPostRepository;
        this.portfolioItemRepository = portfolioItemRepository;
    }

    /**
     * Returns a paginated list of published blog posts, optionally filtered by title search.
     * Page size is capped at 50.
     */
    public Page<BlogPost> getPublishedBlogs(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        if (search != null && !search.isBlank()) {
            return blogPostRepository.findByPublishedTrueAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(search, pageable);
        }
        return blogPostRepository.findByPublishedTrueOrderByPublishedAtDesc(pageable);
    }

    /**
     * Returns a single published blog post by slug, or null if not found.
     */
    public BlogPost getBlogBySlug(String slug) {
        return blogPostRepository.findBySlugAndPublishedTrue(slug);
    }

    /**
     * Returns a paginated list of published portfolio items, optionally filtered by projectType.
     * Page size is capped at 50.
     */
    public Page<PortfolioItem> getPublishedPortfolio(int page, int size, String projectType) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        if (projectType != null && !projectType.isBlank()) {
            return portfolioItemRepository.findByPublishedTrueAndProjectTypeIgnoreCaseOrderByCompletionDateDesc(projectType, pageable);
        }
        return portfolioItemRepository.findByPublishedTrueOrderByCompletionDateDesc(pageable);
    }

    /**
     * Returns a single published portfolio item by slug, or null if not found.
     */
    public PortfolioItem getPortfolioBySlug(String slug) {
        return portfolioItemRepository.findBySlugAndPublishedTrue(slug);
    }
}
