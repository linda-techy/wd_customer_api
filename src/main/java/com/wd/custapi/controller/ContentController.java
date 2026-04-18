package com.wd.custapi.controller;

import com.wd.custapi.model.BlogPost;
import com.wd.custapi.model.PortfolioItem;
import com.wd.custapi.service.ContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Public content endpoints — no authentication required.
 * Path /api/public/** is already permitAll() in SecurityConfig.
 */
@RestController
@RequestMapping("/api/public/content")
public class ContentController {

    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * GET /api/public/content/blogs?page=0&size=10&search=
     * Returns paginated blog summaries (id, title, slug, excerpt, imageUrl, author, publishedAt).
     */
    @GetMapping("/blogs")
    public ResponseEntity<?> getBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        try {
            Page<BlogPost> blogPage = contentService.getPublishedBlogs(page, size, search);
            List<Map<String, Object>> summaries = blogPage.getContent().stream()
                    .map(this::toBlogSummary)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("blogs", summaries);
            response.put("totalElements", blogPage.getTotalElements());
            response.put("totalPages", blogPage.getTotalPages());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching published blogs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve blogs"));
        }
    }

    /**
     * GET /api/public/content/blogs/{slug}
     * Returns full blog post with content. 404 if not found.
     */
    @GetMapping("/blogs/{slug}")
    public ResponseEntity<?> getBlogBySlug(@PathVariable String slug) {
        try {
            BlogPost blog = contentService.getBlogBySlug(slug);
            if (blog == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Blog post not found"));
            }
            return ResponseEntity.ok(toBlogDetail(blog));
        } catch (Exception e) {
            logger.error("Error fetching blog by slug {}: {}", slug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve blog post"));
        }
    }

    /**
     * GET /api/public/content/portfolio?page=0&size=10&projectType=
     * Returns paginated portfolio summaries.
     */
    @GetMapping("/portfolio")
    public ResponseEntity<?> getPortfolio(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String projectType) {
        try {
            Page<PortfolioItem> portfolioPage = contentService.getPublishedPortfolio(page, size, projectType);
            List<Map<String, Object>> summaries = portfolioPage.getContent().stream()
                    .map(this::toPortfolioSummary)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("portfolio", summaries);
            response.put("totalElements", portfolioPage.getTotalElements());
            response.put("totalPages", portfolioPage.getTotalPages());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching published portfolio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve portfolio"));
        }
    }

    /**
     * GET /api/public/content/portfolio/{slug}
     * Returns full portfolio item with gallery. 404 if not found.
     */
    @GetMapping("/portfolio/{slug}")
    public ResponseEntity<?> getPortfolioBySlug(@PathVariable String slug) {
        try {
            PortfolioItem item = contentService.getPortfolioBySlug(slug);
            if (item == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Portfolio item not found"));
            }
            return ResponseEntity.ok(toPortfolioDetail(item));
        } catch (Exception e) {
            logger.error("Error fetching portfolio by slug {}: {}", slug, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve portfolio item"));
        }
    }

    /**
     * GET /api/public/content/live-activities
     * Placeholder — returns empty list.
     */
    @GetMapping("/live-activities")
    public ResponseEntity<?> getLiveActivities() {
        return ResponseEntity.ok(Map.of("activities", List.of()));
    }

    // ─── Mapping helpers ──────────────────────────────────────────────────────

    private Map<String, Object> toBlogSummary(BlogPost blog) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", blog.getId());
        map.put("title", blog.getTitle());
        map.put("slug", blog.getSlug());
        map.put("excerpt", blog.getExcerpt());
        map.put("imageUrl", blog.getImageUrl());
        map.put("author", blog.getAuthor());
        map.put("publishedAt", blog.getPublishedAt());
        return map;
    }

    private Map<String, Object> toBlogDetail(BlogPost blog) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", blog.getId());
        map.put("title", blog.getTitle());
        map.put("slug", blog.getSlug());
        map.put("excerpt", blog.getExcerpt());
        map.put("content", blog.getContent());
        map.put("imageUrl", blog.getImageUrl());
        map.put("author", blog.getAuthor());
        map.put("publishedAt", blog.getPublishedAt());
        map.put("createdAt", blog.getCreatedAt());
        map.put("updatedAt", blog.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toPortfolioSummary(PortfolioItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("title", item.getTitle());
        map.put("slug", item.getSlug());
        map.put("location", item.getLocation());
        map.put("projectType", item.getProjectType());
        map.put("areaSqft", item.getAreaSqft());
        map.put("completionDate", item.getCompletionDate());
        map.put("coverImageUrl", item.getCoverImageUrl());
        return map;
    }

    private Map<String, Object> toPortfolioDetail(PortfolioItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("title", item.getTitle());
        map.put("slug", item.getSlug());
        map.put("description", item.getDescription());
        map.put("location", item.getLocation());
        map.put("projectType", item.getProjectType());
        map.put("areaSqft", item.getAreaSqft());
        map.put("completionDate", item.getCompletionDate());
        map.put("imageUrls", item.getImageUrls() != null ? Arrays.asList(item.getImageUrls()) : List.of());
        map.put("coverImageUrl", item.getCoverImageUrl());
        map.put("createdAt", item.getCreatedAt());
        map.put("updatedAt", item.getUpdatedAt());
        return map;
    }
}
