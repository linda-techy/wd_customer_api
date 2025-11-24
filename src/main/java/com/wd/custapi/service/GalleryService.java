package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GalleryService {
    
    private final GalleryImageRepository galleryImageRepository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    private final SiteReportRepository siteReportRepository;
    private final FileStorageService fileStorageService;
    
    public GalleryService(GalleryImageRepository galleryImageRepository,
                          ProjectRepository projectRepository,
                          CustomerUserRepository userRepository,
                          SiteReportRepository siteReportRepository,
                          FileStorageService fileStorageService) {
        this.galleryImageRepository = galleryImageRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.siteReportRepository = siteReportRepository;
        this.fileStorageService = fileStorageService;
    }
    
    @Transactional
    public GalleryImageDto uploadImage(Long projectId, MultipartFile file, 
                                       GalleryUploadRequest request, Long userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        CustomerUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Store file
        String imagePath = fileStorageService.storeFile(file, "projects/" + projectId + "/gallery");
        
        GalleryImage image = new GalleryImage();
        image.setProject(project);
        image.setImagePath(imagePath);
        image.setCaption(request.caption());
        image.setTakenDate(request.takenDate() != null ? request.takenDate() : LocalDate.now());
        image.setUploadedBy(user);
        image.setLocationTag(request.locationTag());
        
        if (request.tags() != null) {
            image.setTags(request.tags().toArray(new String[0]));
        }
        
        if (request.siteReportId() != null) {
            SiteReport siteReport = siteReportRepository.findById(request.siteReportId())
                .orElseThrow(() -> new RuntimeException("Site report not found"));
            image.setSiteReport(siteReport);
        }
        
        image = galleryImageRepository.save(image);
        return toDto(image);
    }
    
    public List<GalleryImageDto> getProjectImages(Long projectId) {
        return galleryImageRepository.findByProjectIdOrderByTakenDateDesc(projectId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    public List<GalleryImageDto> getImagesByDate(Long projectId, LocalDate date) {
        return galleryImageRepository.findByProjectIdAndTakenDate(projectId, date)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    private GalleryImageDto toDto(GalleryImage image) {
        return new GalleryImageDto(
            image.getId(),
            image.getProject().getId(),
            image.getImagePath(),
            image.getThumbnailPath(),
            image.getCaption(),
            image.getTakenDate(),
            image.getUploadedBy().getId(),
            image.getUploadedBy().getFirstName() + " " + image.getUploadedBy().getLastName(),
            image.getUploadedAt(),
            image.getSiteReport() != null ? image.getSiteReport().getId() : null,
            image.getLocationTag(),
            image.getTags() != null ? Arrays.asList(image.getTags()) : null
        );
    }
}

