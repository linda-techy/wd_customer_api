package com.wd.custapi.service;

import com.wd.custapi.model.BlogPost;
import com.wd.custapi.model.PortfolioItem;
import com.wd.custapi.repository.BlogPostRepository;
import com.wd.custapi.repository.PortfolioItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private PortfolioItemRepository portfolioItemRepository;

    @InjectMocks
    private ContentService contentService;

    // ── getPublishedBlogs ─────────────────────────────────────────────────────

    @Test
    void getPublishedBlogs_noSearch_returnsOnlyPublishedPosts() {
        BlogPost blog = new BlogPost();
        blog.setId(1L);
        blog.setTitle("Our First Project");
        blog.setPublished(true);

        Page<BlogPost> page = new PageImpl<>(List.of(blog));
        when(blogPostRepository.findByPublishedTrueOrderByPublishedAtDesc(any(Pageable.class)))
                .thenReturn(page);

        Page<BlogPost> result = contentService.getPublishedBlogs(0, 10, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("Our First Project", result.getContent().get(0).getTitle());
        verify(blogPostRepository).findByPublishedTrueOrderByPublishedAtDesc(any(Pageable.class));
        verify(blogPostRepository, never())
                .findByPublishedTrueAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(any(), any());
    }

    @Test
    void getPublishedBlogs_withSearch_filtersResultsByTitle() {
        BlogPost blog = new BlogPost();
        blog.setId(2L);
        blog.setTitle("Roof Construction Tips");
        blog.setPublished(true);

        Page<BlogPost> page = new PageImpl<>(List.of(blog));
        when(blogPostRepository.findByPublishedTrueAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
                eq("roof"), any(Pageable.class)))
                .thenReturn(page);

        Page<BlogPost> result = contentService.getPublishedBlogs(0, 10, "roof");

        assertEquals(1, result.getTotalElements());
        verify(blogPostRepository).findByPublishedTrueAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
                eq("roof"), any(Pageable.class));
        verify(blogPostRepository, never()).findByPublishedTrueOrderByPublishedAtDesc(any());
    }

    @Test
    void getPublishedBlogs_requestedSizeOver50_capsAt50() {
        Page<BlogPost> emptyPage = Page.empty();
        when(blogPostRepository.findByPublishedTrueOrderByPublishedAtDesc(any(Pageable.class)))
                .thenReturn(emptyPage);

        contentService.getPublishedBlogs(0, 200, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(blogPostRepository).findByPublishedTrueOrderByPublishedAtDesc(pageableCaptor.capture());

        Pageable captured = pageableCaptor.getValue();
        assertEquals(50, captured.getPageSize());
    }

    // ── getBlogBySlug ─────────────────────────────────────────────────────────

    @Test
    void getBlogBySlug_validPublishedSlug_returnsBlog() {
        BlogPost blog = new BlogPost();
        blog.setId(3L);
        blog.setSlug("wall-builders-tips");
        blog.setPublished(true);

        when(blogPostRepository.findBySlugAndPublishedTrue("wall-builders-tips")).thenReturn(blog);

        BlogPost result = contentService.getBlogBySlug("wall-builders-tips");

        assertNotNull(result);
        assertEquals("wall-builders-tips", result.getSlug());
    }

    @Test
    void getBlogBySlug_unpublishedOrMissingSlug_returnsNull() {
        when(blogPostRepository.findBySlugAndPublishedTrue("draft-post")).thenReturn(null);

        BlogPost result = contentService.getBlogBySlug("draft-post");

        assertNull(result);
    }

    // ── getPublishedPortfolio ─────────────────────────────────────────────────

    @Test
    void getPublishedPortfolio_withProjectTypeFilter_filtersResults() {
        PortfolioItem item = new PortfolioItem();
        item.setId(10L);
        item.setProjectType("RESIDENTIAL");
        item.setPublished(true);

        Page<PortfolioItem> page = new PageImpl<>(List.of(item));
        when(portfolioItemRepository.findByPublishedTrueAndProjectTypeIgnoreCaseOrderByCompletionDateDesc(
                eq("RESIDENTIAL"), any(Pageable.class)))
                .thenReturn(page);

        Page<PortfolioItem> result = contentService.getPublishedPortfolio(0, 10, "RESIDENTIAL");

        assertEquals(1, result.getTotalElements());
        assertEquals("RESIDENTIAL", result.getContent().get(0).getProjectType());
        verify(portfolioItemRepository)
                .findByPublishedTrueAndProjectTypeIgnoreCaseOrderByCompletionDateDesc(eq("RESIDENTIAL"), any());
        verify(portfolioItemRepository, never()).findByPublishedTrueOrderByCompletionDateDesc(any());
    }
}
