package com.wd.custapi.controller;

import com.wd.custapi.model.ProjectDocument;
import com.wd.custapi.repository.ProjectDocumentRepository;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Controller for serving files from storage
 * Endpoint: /api/storage/**
 */
@RestController
@RequestMapping("/api/storage")
public class FileDownloadController {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloadController.class);

    @Value("${storageBasePath}")
    private String storageBasePath;

    @Autowired
    private ProjectDocumentRepository projectDocumentRepository;

    @Autowired
    private DashboardService dashboardService;

    /**
     * Serve files from storage path
     * GET /api/storage/projects/1/documents/file.pdf
     * 
     * @param request - The HTTP request
     * @param download - Whether to force download
     * @param rangeHeader - Range header for streaming
     * @return File as Resource with appropriate content type
     */
    @GetMapping("/**")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ARCHITECT', 'INTERIOR_DESIGNER', 'SITE_ENGINEER', 'VIEWER')")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request,
                                               @RequestParam(required = false) String download,
                                               @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            // Get the full request path (everything after /api/storage/)
            String requestURI = request.getRequestURI();
            String requestPath;
            
            if (requestURI.startsWith("/api/storage/")) {
                requestPath = requestURI.substring("/api/storage/".length());
            } else if (requestURI.startsWith("/api/storage")) {
                requestPath = requestURI.substring("/api/storage".length());
                // Remove leading slash if present
                if (requestPath.startsWith("/")) {
                    requestPath = requestPath.substring(1);
                }
            } else {
                // Fallback: try to extract from path info
                String pathInfo = request.getPathInfo();
                requestPath = (pathInfo != null && pathInfo.startsWith("/")) 
                    ? pathInfo.substring(1) 
                    : (pathInfo != null ? pathInfo : "");
            }
            
            // Validate request path is not empty
            if (requestPath == null || requestPath.isEmpty()) {
                logger.warn("Empty request path for URI: {}", requestURI);
                return ResponseEntity.badRequest().build();
            }
            
            // URL decode the path to handle special characters
            try {
                requestPath = URLDecoder.decode(requestPath, StandardCharsets.UTF_8);
            } catch (Exception e) {
                logger.warn("Failed to decode request path '{}': {}", requestPath, e.getMessage());
                // Continue with original path if decoding fails
            }

            // Ownership check — file must be linked to a project the caller owns
            String authenticatedEmail = currentUserEmail();
            if (resolveOwnedDocument(requestPath, authenticatedEmail).isEmpty()) {
                logger.info("Denied file access by {} to {} (no owned ProjectDocument match)", authenticatedEmail, requestPath);
                return ResponseEntity.notFound().build();
            }

            logger.debug("File download request - URI: {}, Path: {}", requestURI, requestPath);

            // Build full file path
            Path filePath = Paths.get(storageBasePath).resolve(requestPath).normalize();

            // Security check - ensure file is within storage directory
            Path basePath = Paths.get(storageBasePath).toAbsolutePath().normalize();
            Path normalizedFilePath = filePath.toAbsolutePath().normalize();
            if (!normalizedFilePath.startsWith(basePath)) {
                logger.warn("Path traversal attempt blocked: {} (resolved to {})", requestPath, normalizedFilePath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Check if file exists
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                logger.debug("File not found or not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Build response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            
            // Handle download vs inline display
            if ("true".equals(download)) {
                headers.setContentDispositionFormData("attachment", resource.getFilename());
            } else {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"");
            }

            // Private files — prevent caching in proxies, CDNs, and shared environments
            headers.setCacheControl("private, no-store, max-age=0");

            // Handle range requests for video/audio streaming
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                long fileSize = Files.size(filePath);
                return handleRangeRequest(resource, rangeHeader, fileSize, contentType, requestPath);
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (MalformedURLException e) {
            logger.error("Malformed URL for file request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            logger.error("IO error serving file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Unexpected error serving file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handle range requests for streaming (videos, large PDFs)
     */
    private ResponseEntity<Resource> handleRangeRequest(Resource resource, String rangeHeader, 
                                                         long fileSize, String contentType, String fileName) {
        try {
            String[] ranges = rangeHeader.substring(6).split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 && !ranges[1].isEmpty() 
                    ? Long.parseLong(ranges[1]) 
                    : fileSize - 1;

            if (start >= fileSize || end >= fileSize) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }

            long contentLength = end - start + 1;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setContentLength(contentLength);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(resource);

        } catch (NumberFormatException e) {
            logger.warn("Invalid range header format for file {}: {}", fileName, rangeHeader);
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        } catch (Exception e) {
            logger.error("Range request failed for file {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get file metadata without downloading
     * HEAD /api/storage/projects/1/documents/file.pdf
     */
    @RequestMapping(value = "/**", method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ARCHITECT', 'INTERIOR_DESIGNER', 'SITE_ENGINEER', 'VIEWER')")
    public ResponseEntity<Void> getFileMetadata(HttpServletRequest request) {
        try {
            // Get the full request path (everything after /api/storage/)
            String requestURI = request.getRequestURI();
            String requestPath;
            
            if (requestURI.startsWith("/api/storage/")) {
                requestPath = requestURI.substring("/api/storage/".length());
            } else if (requestURI.startsWith("/api/storage")) {
                requestPath = requestURI.substring("/api/storage".length());
                // Remove leading slash if present
                if (requestPath.startsWith("/")) {
                    requestPath = requestPath.substring(1);
                }
            } else {
                // Fallback: try to extract from path info
                String pathInfo = request.getPathInfo();
                requestPath = (pathInfo != null && pathInfo.startsWith("/")) 
                    ? pathInfo.substring(1) 
                    : (pathInfo != null ? pathInfo : "");
            }
            
            // Validate request path is not empty
            if (requestPath == null || requestPath.isEmpty()) {
                logger.warn("Empty request path for HEAD request: {}", requestURI);
                return ResponseEntity.badRequest().build();
            }
            
            // URL decode the path to handle special characters
            try {
                requestPath = URLDecoder.decode(requestPath, StandardCharsets.UTF_8);
            } catch (Exception e) {
                logger.warn("Failed to decode path for HEAD request '{}': {}", requestPath, e.getMessage());
            }

            // Ownership check — file must be linked to a project the caller owns
            String authenticatedEmail = currentUserEmail();
            if (resolveOwnedDocument(requestPath, authenticatedEmail).isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(storageBasePath).resolve(requestPath).normalize();

            // Security check - ensure file is within storage directory
            Path basePath = Paths.get(storageBasePath).toAbsolutePath().normalize();
            Path normalizedFilePath = filePath.toAbsolutePath().normalize();
            if (!normalizedFilePath.startsWith(basePath)) {
                logger.warn("Path traversal attempt blocked on HEAD: {} (resolved to {})", requestPath, normalizedFilePath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(resource.contentLength());
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");

            return ResponseEntity.ok().headers(headers).build();

        } catch (Exception e) {
            logger.error("Error getting file metadata: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Optional<ProjectDocument> resolveOwnedDocument(String requestPath, String authenticatedEmail) {
        List<ProjectDocument> candidates = projectDocumentRepository.findByFilePath(requestPath);
        for (ProjectDocument doc : candidates) {
            if (!ProjectDocumentRepository.REFERENCE_TYPE_PROJECT.equals(doc.getReferenceType())) {
                continue;
            }
            try {
                dashboardService.getProjectByIdAndEmail(doc.getReferenceId(), authenticatedEmail);
                return Optional.of(doc);
            } catch (RuntimeException notOwned) {
                // keep scanning — another candidate row might be owned by this user
            }
        }
        return Optional.empty();
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No authenticated user");
        }
        return auth.getName();
    }
}
