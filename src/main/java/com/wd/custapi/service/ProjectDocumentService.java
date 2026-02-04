package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectDocumentService {

    private static final String REFERENCE_TYPE_PROJECT = "PROJECT";

    private final ProjectDocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final DocumentCategoryRepository categoryRepository;
    private final CustomerUserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ActivityFeedService activityFeedService;

    public ProjectDocumentService(ProjectDocumentRepository documentRepository,
                                  ProjectRepository projectRepository,
                                  DocumentCategoryRepository categoryRepository,
                                  CustomerUserRepository userRepository,
                                  FileStorageService fileStorageService,
                                  ActivityFeedService activityFeedService) {
        this.documentRepository = documentRepository;
        this.projectRepository = projectRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.activityFeedService = activityFeedService;
    }

    @Transactional
    public ProjectDocumentDto uploadDocument(Long projectId, MultipartFile file,
                                             DocumentUploadRequest request, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        DocumentCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        CustomerUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String filePath = fileStorageService.storeFile(file, "projects/" + projectId + "/documents");

        ProjectDocument document = new ProjectDocument();
        document.setReferenceId(project.getId());
        document.setReferenceType(REFERENCE_TYPE_PROJECT);
        document.setCategory(category);
        document.setFilename(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setFileType(file.getContentType());
        document.setCreatedBy(user);
        document.setCreatedAt(java.time.LocalDateTime.now());
        document.setDescription(request.description());

        document = documentRepository.save(document);

        activityFeedService.createActivity(projectId, "DOCUMENT_UPLOADED",
                "Document uploaded: " + file.getOriginalFilename(), null, userId);

        return toDto(document);
    }

    public List<ProjectDocumentDto> getProjectDocuments(Long projectId, Long categoryId) {
        List<ProjectDocument> documents;
        if (categoryId != null) {
            documents = documentRepository.findAllByReferenceIdAndReferenceTypeAndCategoryId(
                    projectId, REFERENCE_TYPE_PROJECT, categoryId);
        } else {
            documents = documentRepository.findAllByReferenceIdAndReferenceType(projectId, REFERENCE_TYPE_PROJECT);
        }
        return documents.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<DocumentCategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(c -> new DocumentCategoryDto(c.getId(), c.getName(), c.getDescription(), c.getDisplayOrder()))
                .collect(Collectors.toList());
    }

    private ProjectDocumentDto toDto(ProjectDocument doc) {
        String downloadUrl = "https://cust-api.walldotbuilders.com/api/storage/" + doc.getFilePath();
        Long projectId = "PROJECT".equals(doc.getReferenceType()) ? doc.getReferenceId() : null;
        Long uploadedById = doc.getCreatedBy() != null ? doc.getCreatedBy().getId() : null;
        String uploadedByName = doc.getCreatedBy() != null
                ? doc.getCreatedBy().getFirstName() + " " + doc.getCreatedBy().getLastName()
                : "Company";

        return new ProjectDocumentDto(
                doc.getId(),
                projectId,
                doc.getCategory() != null ? doc.getCategory().getId() : null,
                doc.getCategory() != null ? doc.getCategory().getName() : "Uncategorized",
                doc.getFilename(),
                doc.getFilePath(),
                downloadUrl,
                doc.getFileSize(),
                doc.getFileType(),
                uploadedById,
                uploadedByName,
                doc.getCreatedAt(),
                doc.getDescription(),
                doc.getVersion(),
                doc.getIsActive()
        );
    }
}
