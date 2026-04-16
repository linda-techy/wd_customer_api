package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectDocument;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectDocumentRepositoryFilePathTest extends TestcontainersPostgresBase {

    @Autowired
    private ProjectDocumentRepository repo;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void findByFilePath_returnsMatchingRow() {
        jdbc.update(
                "INSERT INTO document_categories (id, name, created_at) "
              + "VALUES (9001, 'test-cat', now()) ON CONFLICT DO NOTHING");
        jdbc.update(
                "INSERT INTO project_documents "
              + "(reference_id, reference_type, category_id, filename, file_path, "
              + " created_at, is_active, uploaded_by_type) "
              + "VALUES (12345, 'PROJECT', 9001, 'find-me.pdf', "
              + "        'projects/12345/documents/find-me.pdf', now(), true, 'CUSTOMER')");

        List<ProjectDocument> found = repo.findByFilePath("projects/12345/documents/find-me.pdf");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getFilename()).isEqualTo("find-me.pdf");
        assertThat(found.get(0).getReferenceId()).isEqualTo(12345L);
        assertThat(found.get(0).getReferenceType()).isEqualTo("PROJECT");
    }

    @Test
    void findByFilePath_returnsEmptyWhenNoMatch() {
        List<ProjectDocument> found = repo.findByFilePath("projects/99999/never-existed.pdf");
        assertThat(found).isEmpty();
    }
}
