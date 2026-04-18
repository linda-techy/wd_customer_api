package com.wd.custapi.controller;

import com.wd.custapi.model.BlogPost;
import com.wd.custapi.model.PortfolioItem;
import com.wd.custapi.service.ContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContentController — direct method calls to avoid complex SecurityConfig wiring.
 */
@ExtendWith(MockitoExtension.class)
class ContentControllerTest {

    @Mock
    private ContentService contentService;

    @InjectMocks
    private ContentController contentController;

    private BlogPost publishedBlog;
    private PortfolioItem publishedPortfolioItem;

    @BeforeEach
    void setUp() {
        publishedBlog = new BlogPost();
        publishedBlog.setId(1L);
        publishedBlog.setTitle("Wall Construction Guide");
        publishedBlog.setSlug("wall-construction-guide");
        publishedBlog.setExcerpt("A comprehensive guide");
        publishedBlog.setContent("Full content here...");
        publishedBlog.setAuthor("Nithin");
        publishedBlog.setPublished(true);
        publishedBlog.setPublishedAt(LocalDateTime.now().minusDays(1));
        publishedBlog.setCreatedAt(LocalDateTime.now().minusDays(5));
        publishedBlog.setUpdatedAt(LocalDateTime.now().minusDays(1));

        publishedPortfolioItem = new PortfolioItem();
        publishedPortfolioItem.setId(10L);
        publishedPortfolioItem.setTitle("Modern Villa");
        publishedPortfolioItem.setSlug("modern-villa");
        publishedPortfolioItem.setProjectType("RESIDENTIAL");
        publishedPortfolioItem.setPublished(true);
    }

    // ── GET /api/public/content/blogs ─────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getBlogs_returns200WithPaginatedList() {
        Page<BlogPost> page = new PageImpl<>(List.of(publishedBlog));
        when(contentService.getPublishedBlogs(0, 10, null)).thenReturn(page);

        ResponseEntity<?> response = contentController.getBlogs(0, 10, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("blogs"));
        List<?> blogs = (List<?>) body.get("blogs");
        assertEquals(1, blogs.size());
        assertEquals(1L, ((Map<?, ?>) blogs.get(0)).get("id"));
    }

    // ── GET /api/public/content/blogs/{slug} ──────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getBlogBySlug_validSlug_returns200WithDetail() {
        when(contentService.getBlogBySlug("wall-construction-guide")).thenReturn(publishedBlog);

        ResponseEntity<?> response = contentController.getBlogBySlug("wall-construction-guide");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Wall Construction Guide", body.get("title"));
        assertEquals("wall-construction-guide", body.get("slug"));
        assertTrue(body.containsKey("content"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBlogBySlug_invalidSlug_returns404() {
        when(contentService.getBlogBySlug("nonexistent-slug")).thenReturn(null);

        ResponseEntity<?> response = contentController.getBlogBySlug("nonexistent-slug");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("error"));
    }

    // ── GET /api/public/content/portfolio ─────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getPortfolio_returns200WithList() {
        Page<PortfolioItem> page = new PageImpl<>(List.of(publishedPortfolioItem));
        when(contentService.getPublishedPortfolio(0, 10, null)).thenReturn(page);

        ResponseEntity<?> response = contentController.getPortfolio(0, 10, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("portfolio"));
        List<?> items = (List<?>) body.get("portfolio");
        assertEquals(1, items.size());
    }

    // ── GET /api/public/content/portfolio/{slug} ──────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getPortfolioBySlug_missingSlug_returns404() {
        when(contentService.getPortfolioBySlug("missing-villa")).thenReturn(null);

        ResponseEntity<?> response = contentController.getPortfolioBySlug("missing-villa");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("error"));
    }

    // ── GET /api/public/content/live-activities ───────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void getLiveActivities_returns200WithEmptyList() {
        ResponseEntity<?> response = contentController.getLiveActivities();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("activities"));
        List<?> activities = (List<?>) body.get("activities");
        assertTrue(activities.isEmpty());
        // Verify no service calls — endpoint is a placeholder
        verifyNoInteractions(contentService);
    }
}
