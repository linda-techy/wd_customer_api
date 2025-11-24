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
        
        // Store file
        String filePath = fileStorageService.storeFile(file, "projects/" + projectId + "/documents");
        
        ProjectDocument document = new ProjectDocument();
        document.setProject(project);
        document.setCategory(category);
        document.setFilename(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setFileType(file.getContentType());
        document.setUploadedBy(user);
        document.setDescription(request.description());
        
        document = documentRepository.save(document);
        
        // Create activity feed
        activityFeedService.createActivity(projectId, "DOCUMENT_UPLOADED", 
            "Document uploaded: " + file.getOriginalFilename(), null, userId);
        
        return toDto(document);
    }
    
    public List<ProjectDocumentDto> getProjectDocuments(Long projectId, Long categoryId) {
        List<ProjectDocument> documents;
        if (categoryId != null) {
            // Get ALL documents for the project and category regardless of isActive status
            // This ensures documents are returned even if isActive is false or NULL
            documents = documentRepository.findAllByProjectIdAndCategoryId(projectId, categoryId);
        } else {
            // Get ALL documents for the project regardless of isActive status
            // This ensures documents are returned even if isActive is false or NULL
            documents = documentRepository.findAllByProjectId(projectId);
        }
        return documents.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    public List<DocumentCategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
            .map(c -> new DocumentCategoryDto(c.getId(), c.getName(), c.getDescription(), c.getDisplayOrder()))
            .collect(Collectors.toList());
    }
    
    private ProjectDocumentDto toDto(ProjectDocument doc) {
        // Generate full download URL - goes through authenticated /api/storage/ endpoint
        String downloadUrl = "https://cust-api.walldotbuilders.com/api/storage/" + doc.getFilePath();
        
        return new ProjectDocumentDto(
            doc.getId(),
            doc.getProject().getId(),
            doc.getCategory().getId(),
            doc.getCategory().getName(),
            doc.getFilename(),
            doc.getFilePath(),
            downloadUrl,  // Full URL for downloading/viewing
            doc.getFileSize(),
            doc.getFileType(),
            doc.getUploadedBy().getId(),
            doc.getUploadedBy().getFirstName() + " " + doc.getUploadedBy().getLastName(),
            doc.getUploadDate(),
            doc.getDescription(),
            doc.getVersion(),
            doc.getIsActive()
        );
    }
}

