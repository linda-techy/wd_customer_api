package com.wd.custapi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for serving files from storage
 * Endpoint: /api/storage/**
 */
@RestController
@RequestMapping("/api/storage")
public class FileDownloadController {

    @Value("${storageBasePath}")
    private String storageBasePath;

    /**
     * Serve files from storage path
     * GET /api/storage/projects/1/documents/file.pdf
     * 
     * @param filePath - The relative path after /api/storage/
     * @return File as Resource with appropriate content type
     */
    @GetMapping("/**")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
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
                System.err.println("Empty request path for URI: " + requestURI);
                return ResponseEntity.badRequest().build();
            }
            
            // URL decode the path to handle special characters
            try {
                requestPath = URLDecoder.decode(requestPath, StandardCharsets.UTF_8);
            } catch (Exception e) {
                System.err.println("Failed to decode request path: " + requestPath);
                // Continue with original path if decoding fails
            }

            // Debug logging
            System.out.println("=== File Download Request ===");
            System.out.println("Storage Base Path: " + storageBasePath);
            System.out.println("Request URI: " + requestURI);
            System.out.println("Request Path: " + requestPath);

            // Build full file path
            Path filePath = Paths.get(storageBasePath).resolve(requestPath).normalize();
            System.out.println("Resolved File Path: " + filePath.toString());
            System.out.println("File Exists: " + java.nio.file.Files.exists(filePath));
            System.out.println("============================");

            // Security check - ensure file is within storage directory
            Path basePath = Paths.get(storageBasePath).toAbsolutePath().normalize();
            Path normalizedFilePath = filePath.toAbsolutePath().normalize();
            if (!normalizedFilePath.startsWith(basePath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Check if file exists
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
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

            // Add cache control for better performance
            headers.setCacheControl("public, max-age=31536000");

            // Handle range requests for video/audio streaming
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                long fileSize = Files.size(filePath);
                return handleRangeRequest(resource, rangeHeader, fileSize, contentType);
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .body(resource);

        } catch (MalformedURLException e) {
            System.err.println("MalformedURLException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handle range requests for streaming (videos, large PDFs)
     */
    private ResponseEntity<Resource> handleRangeRequest(Resource resource, String rangeHeader, 
                                                         long fileSize, String contentType) {
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

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get file metadata without downloading
     * HEAD /api/storage/projects/1/documents/file.pdf
     */
    @RequestMapping(value = "/**", method = RequestMethod.HEAD)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
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
                return ResponseEntity.badRequest().build();
            }
            
            // URL decode the path to handle special characters
            try {
                requestPath = URLDecoder.decode(requestPath, StandardCharsets.UTF_8);
            } catch (Exception e) {
                // Continue with original path if decoding fails
            }

            Path filePath = Paths.get(storageBasePath).resolve(requestPath).normalize();

            // Security check - ensure file is within storage directory
            Path basePath = Paths.get(storageBasePath).toAbsolutePath().normalize();
            Path normalizedFilePath = filePath.toAbsolutePath().normalize();
            if (!normalizedFilePath.startsWith(basePath)) {
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}


