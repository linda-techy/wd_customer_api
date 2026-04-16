package com.wd.custapi.controller;

import com.wd.custapi.service.JwtService;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class FileDownloadIdorIntegrationTest extends TestcontainersPostgresBase {

    @TempDir
    static Path storageTempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    private String aliceToken;
    private String bobToken;

    @DynamicPropertySource
    static void overrideStoragePath(DynamicPropertyRegistry registry) {
        registry.add("storageBasePath", () -> storageTempDir.toAbsolutePath().toString());
    }

    @BeforeEach
    void setUp() throws Exception {
        // Clean up previous test data (order matters due to FK constraints)
        jdbc.update("DELETE FROM project_documents");
        jdbc.update("DELETE FROM project_members");
        jdbc.update("DELETE FROM customer_projects");
        jdbc.update("DELETE FROM customer_users");

        // Seed roles
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");

        // Seed Alice
        jdbc.update("INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                + "VALUES ('alice@test.com', 'x', 'Alice', 1, now(), true)");
        Long aliceId = jdbc.queryForObject("SELECT id FROM customer_users WHERE email = 'alice@test.com'", Long.class);

        // Seed Bob
        jdbc.update("INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                + "VALUES ('bob@test.com', 'x', 'Bob', 1, now(), true)");
        Long bobId = jdbc.queryForObject("SELECT id FROM customer_users WHERE email = 'bob@test.com'", Long.class);

        // Seed Alice's project
        Long aliceProjectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) VALUES ('Alice Project', gen_random_uuid(), 0) RETURNING id",
                Long.class);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)", aliceProjectId, aliceId);

        // Seed Bob's project
        Long bobProjectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) VALUES ('Bob Project', gen_random_uuid(), 0) RETURNING id",
                Long.class);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)", bobProjectId, bobId);

        // Seed document category
        jdbc.update("INSERT INTO document_categories (id, name, created_at) VALUES (9001, 'test-cat', now()) ON CONFLICT DO NOTHING");

        // Seed Alice's project_document
        String aliceFilePath = "projects/" + aliceProjectId + "/documents/alice-file.pdf";
        jdbc.update("INSERT INTO project_documents (reference_id, reference_type, category_id, filename, file_path, created_at, is_active, uploaded_by_type) "
                + "VALUES (?, 'PROJECT', 9001, 'alice-file.pdf', ?, now(), true, 'CUSTOMER')",
                aliceProjectId, aliceFilePath);

        // Seed Bob's project_document
        String bobFilePath = "projects/" + bobProjectId + "/documents/bob-file.pdf";
        jdbc.update("INSERT INTO project_documents (reference_id, reference_type, category_id, filename, file_path, created_at, is_active, uploaded_by_type) "
                + "VALUES (?, 'PROJECT', 9001, 'bob-file.pdf', ?, now(), true, 'CUSTOMER')",
                bobProjectId, bobFilePath);

        // Create actual files on disk
        Path aliceDiskDir = storageTempDir.resolve("projects/" + aliceProjectId + "/documents");
        Files.createDirectories(aliceDiskDir);
        Files.writeString(aliceDiskDir.resolve("alice-file.pdf"), "alice-content");

        Path bobDiskDir = storageTempDir.resolve("projects/" + bobProjectId + "/documents");
        Files.createDirectories(bobDiskDir);
        Files.writeString(bobDiskDir.resolve("bob-file.pdf"), "bob-content");

        // Create an orphan file on disk (no DB row)
        Path orphanDir = storageTempDir.resolve("projects/999/documents");
        Files.createDirectories(orphanDir);
        Files.writeString(orphanDir.resolve("orphan.pdf"), "orphan-content");

        // Create a video file for Alice (for range-request test)
        String aliceVideoPath = "projects/" + aliceProjectId + "/documents/alice-video.mp4";
        jdbc.update("INSERT INTO project_documents (reference_id, reference_type, category_id, filename, file_path, created_at, is_active, uploaded_by_type) "
                + "VALUES (?, 'PROJECT', 9001, 'alice-video.mp4', ?, now(), true, 'CUSTOMER')",
                aliceProjectId, aliceVideoPath);
        Files.writeString(aliceDiskDir.resolve("alice-video.mp4"), "fake-video-content-for-range-test");

        // Generate JWT tokens
        aliceToken = jwtService.generateCustomerToken("alice@test.com", new HashMap<>());
        bobToken = jwtService.generateCustomerToken("bob@test.com", new HashMap<>());

        // Store paths for use in tests
        this.aliceFileRelPath = aliceFilePath;
        this.bobFileRelPath = bobFilePath;
        this.aliceVideoRelPath = aliceVideoPath;
    }

    private String aliceFileRelPath;
    private String bobFileRelPath;
    private String aliceVideoRelPath;

    // Case 1: Alice GETs her own file -> 200
    @Test
    void aliceGetsOwnFile_returns200() throws Exception {
        mockMvc.perform(get("/api/storage/" + aliceFileRelPath)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isOk());
    }

    // Case 2: Alice GETs Bob's file -> 404 (IDOR fix)
    @Test
    void aliceGetsBobFile_returns404() throws Exception {
        mockMvc.perform(get("/api/storage/" + bobFileRelPath)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isNotFound());
    }

    // Case 3: Unauthenticated GET -> 401 or 403 (Spring Security rejects without valid token)
    @Test
    void unauthenticatedGet_returnsUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/storage/" + aliceFileRelPath))
                .andExpect(status().isForbidden());
    }

    // Case 4: Alice GETs non-existent path -> 404
    @Test
    void aliceGetsNonExistentPath_returns404() throws Exception {
        mockMvc.perform(get("/api/storage/projects/999999/documents/nope.pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isNotFound());
    }

    // Case 5: Path traversal attempt -> blocked (400 from Spring path normalization or 404 from ownership check)
    @Test
    void pathTraversalAttempt_isBlocked() throws Exception {
        // Spring normalizes ../../ out of the URI before it reaches the controller,
        // which can result in an empty path (400) or a non-matching path (404).
        // Either way the file is NOT served. Verify it's not 200.
        int status = mockMvc.perform(get("/api/storage/../../etc/passwd")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andReturn().getResponse().getStatus();
        // Must not be 200 (file served) — 400 or 404 are both acceptable
        org.assertj.core.api.Assertions.assertThat(status).isIn(400, 404);
    }

    // Case 6: File exists on disk but no DB row -> 404
    @Test
    void fileOnDiskButNoDbRow_returns404() throws Exception {
        mockMvc.perform(get("/api/storage/projects/999/documents/orphan.pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isNotFound());
    }

    // Case 7: Alice HEAD on Bob's file -> 404 (IDOR fix)
    @Test
    void aliceHeadBobFile_returns404() throws Exception {
        mockMvc.perform(head("/api/storage/" + bobFileRelPath)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isNotFound());
    }

    // Case 8: Alice range-request on her own video -> 206
    @Test
    void aliceRangeRequestOwnVideo_returns206() throws Exception {
        mockMvc.perform(get("/api/storage/" + aliceVideoRelPath)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken)
                        .header("Range", "bytes=0-9"))
                .andExpect(status().isPartialContent());
    }
}
