package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.DocumentCategoryDto;
import com.wd.custapi.dto.ProjectModuleDtos.DocumentUploadRequest;
import com.wd.custapi.dto.ProjectModuleDtos.ProjectDocumentDto;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.DocumentCategory;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.ProjectDocument;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.DocumentCategoryRepository;
import com.wd.custapi.repository.ProjectDocumentRepository;
import com.wd.custapi.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure-Mockito unit tests for {@link ProjectDocumentService}. No Spring / DB.
 * Covers uploadDocument (happy path + project/category/user not-found branches),
 * getProjectDocuments (with and without category filter), getAllCategories, and the
 * toDto mapping (downloadUrl, "Company" attribution, null-category fallback).
 */
@ExtendWith(MockitoExtension.class)
class ProjectDocumentServiceTest {

    @Mock private ProjectDocumentRepository documentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private DocumentCategoryRepository categoryRepository;
    @Mock private CustomerUserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ActivityFeedService activityFeedService;

    @InjectMocks private ProjectDocumentService service;

    private DocumentCategory category(Long id, String name) {
        DocumentCategory c = new DocumentCategory();
        c.setId(id);
        c.setName(name);
        c.setDescription("desc-" + id);
        c.setDisplayOrder(2);
        return c;
    }

    // ── uploadDocument ────────────────────────────────────────────────────────

    @Test
    void uploadDocument_happyPath_storesSavesLogsActivityAndMaps() {
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(50L);
        DocumentCategory category = category(7L, "Drawings");
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("plan.pdf");
        when(file.getSize()).thenReturn(2048L);
        when(file.getContentType()).thenReturn("application/pdf");

        when(projectRepository.findById(50L)).thenReturn(Optional.of(project));
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));
        when(userRepository.findById(3L)).thenReturn(Optional.of(mock(CustomerUser.class)));
        when(fileStorageService.storeFile(file, "projects/50/documents"))
                .thenReturn("projects/50/documents/uuid.pdf");
        when(documentRepository.save(any(ProjectDocument.class))).thenAnswer(inv -> {
            ProjectDocument d = inv.getArgument(0);
            d.setId(123L);
            return d;
        });

        ProjectDocumentDto dto = service.uploadDocument(50L, file,
                new DocumentUploadRequest(7L, "Ground floor plan"), 3L);

        assertThat(dto.id()).isEqualTo(123L);
        assertThat(dto.projectId()).isEqualTo(50L);
        assertThat(dto.categoryId()).isEqualTo(7L);
        assertThat(dto.categoryName()).isEqualTo("Drawings");
        assertThat(dto.filename()).isEqualTo("plan.pdf");
        assertThat(dto.filePath()).isEqualTo("projects/50/documents/uuid.pdf");
        assertThat(dto.downloadUrl()).isEqualTo("/api/storage/projects/50/documents/uuid.pdf");
        assertThat(dto.fileSize()).isEqualTo(2048L);
        assertThat(dto.fileType()).isEqualTo("application/pdf");
        assertThat(dto.uploadedByName()).isEqualTo("Company");
        assertThat(dto.description()).isEqualTo("Ground floor plan");

        // entity persisted with PROJECT reference + category
        ArgumentCaptor<ProjectDocument> captor = ArgumentCaptor.forClass(ProjectDocument.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getReferenceType()).isEqualTo("PROJECT");
        assertThat(captor.getValue().getReferenceId()).isEqualTo(50L);

        // activity feed best-effort logged
        verify(activityFeedService).createActivity(eq(50L), eq("DOCUMENT_UPLOADED"),
                contains("plan.pdf"), isNull(), eq(3L));
    }

    @Test
    void uploadDocument_projectNotFound_throwsAndDoesNotStore() {
        when(projectRepository.findById(50L)).thenReturn(Optional.empty());
        MultipartFile file = mock(MultipartFile.class);
        DocumentUploadRequest request = new DocumentUploadRequest(7L, "d");

        assertThatThrownBy(() -> service.uploadDocument(50L, file, request, 3L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project not found");

        verifyNoInteractions(fileStorageService);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_categoryNotFound_throws() {
        when(projectRepository.findById(50L)).thenReturn(Optional.of(mock(Project.class)));
        when(categoryRepository.findById(7L)).thenReturn(Optional.empty());
        MultipartFile file = mock(MultipartFile.class);
        DocumentUploadRequest request = new DocumentUploadRequest(7L, "d");

        assertThatThrownBy(() -> service.uploadDocument(50L, file, request, 3L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found");

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadDocument_userNotFound_throws() {
        when(projectRepository.findById(50L)).thenReturn(Optional.of(mock(Project.class)));
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category(7L, "Drawings")));
        when(userRepository.findById(3L)).thenReturn(Optional.empty());
        MultipartFile file = mock(MultipartFile.class);
        DocumentUploadRequest request = new DocumentUploadRequest(7L, "d");

        assertThatThrownBy(() -> service.uploadDocument(50L, file, request, 3L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verifyNoInteractions(fileStorageService);
    }

    // ── getProjectDocuments ───────────────────────────────────────────────────

    @Test
    void getProjectDocuments_withCategory_usesCategoryFilteredQuery() {
        ProjectDocument doc = document(1L, category(7L, "Drawings"));
        when(documentRepository.findByReferenceIdAndReferenceTypeAndCategoryIdAndIsActiveTrue(50L, "PROJECT", 7L))
                .thenReturn(List.of(doc));

        List<ProjectDocumentDto> result = service.getProjectDocuments(50L, 7L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryName()).isEqualTo("Drawings");
        verify(documentRepository, never())
                .findByReferenceIdAndReferenceTypeAndIsActiveTrue(anyLong(), anyString());
    }

    @Test
    void getProjectDocuments_noCategory_usesUnfilteredActiveQuery() {
        when(documentRepository.findByReferenceIdAndReferenceTypeAndIsActiveTrue(50L, "PROJECT"))
                .thenReturn(List.of(document(1L, category(7L, "Drawings")),
                        document(2L, null))); // null category → "Uncategorized"

        List<ProjectDocumentDto> result = service.getProjectDocuments(50L, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ProjectDocumentDto::categoryName)
                .containsExactly("Drawings", "Uncategorized");
    }

    @Test
    void getProjectDocuments_empty_returnsEmpty() {
        when(documentRepository.findByReferenceIdAndReferenceTypeAndIsActiveTrue(50L, "PROJECT"))
                .thenReturn(List.of());
        assertThat(service.getProjectDocuments(50L, null)).isEmpty();
    }

    // ── getAllCategories ──────────────────────────────────────────────────────

    @Test
    void getAllCategories_mapsEntitiesToDtos() {
        when(categoryRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(category(1L, "A"), category(2L, "B")));

        List<DocumentCategoryDto> result = service.getAllCategories();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("A");
        assertThat(result.get(0).displayOrder()).isEqualTo(2);
        assertThat(result.get(1).name()).isEqualTo("B");
    }

    @Test
    void getAllCategories_empty_returnsEmpty() {
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());
        assertThat(service.getAllCategories()).isEmpty();
    }

    private ProjectDocument document(Long id, DocumentCategory category) {
        ProjectDocument d = new ProjectDocument();
        d.setId(id);
        d.setReferenceType("PROJECT");
        d.setReferenceId(50L);
        d.setCategory(category);
        d.setFilename("file-" + id + ".pdf");
        d.setFilePath("projects/50/documents/file-" + id + ".pdf");
        d.setFileSize(100L);
        d.setFileType("application/pdf");
        d.setDescription("desc");
        return d;
    }
}
