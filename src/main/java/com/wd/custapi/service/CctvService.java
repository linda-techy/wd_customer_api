package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CctvService {
    
    private final CctvCameraRepository cctvRepository;
    private final ProjectRepository projectRepository;
    
    public CctvService(CctvCameraRepository cctvRepository,
                       ProjectRepository projectRepository) {
        this.cctvRepository = cctvRepository;
        this.projectRepository = projectRepository;
    }
    
    @Transactional
    public CctvCameraDto addCamera(Long projectId, CctvCameraRequest request) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        CctvCamera camera = new CctvCamera();
        camera.setProject(project);
        camera.setCameraName(request.cameraName());
        camera.setLocation(request.location());
        camera.setStreamUrl(request.streamUrl());
        camera.setSnapshotUrl(request.snapshotUrl());
        camera.setIsInstalled(request.isInstalled());
        camera.setCameraType(request.cameraType());
        camera.setResolution(request.resolution());
        camera.setNotes(request.notes());
        camera.setLastActive(LocalDateTime.now());
        
        camera = cctvRepository.save(camera);
        return toDto(camera);
    }
    
    @Transactional
    public CctvCameraDto updateCamera(Long cameraId, CctvCameraRequest request) {
        CctvCamera camera = cctvRepository.findById(cameraId)
            .orElseThrow(() -> new RuntimeException("Camera not found"));
        
        camera.setCameraName(request.cameraName());
        camera.setLocation(request.location());
        camera.setStreamUrl(request.streamUrl());
        camera.setSnapshotUrl(request.snapshotUrl());
        camera.setIsInstalled(request.isInstalled());
        camera.setCameraType(request.cameraType());
        camera.setResolution(request.resolution());
        camera.setNotes(request.notes());
        
        camera = cctvRepository.save(camera);
        return toDto(camera);
    }
    
    public List<CctvCameraDto> getProjectCameras(Long projectId) {
        return cctvRepository.findByProjectId(projectId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    public List<CctvCameraDto> getInstalledCameras(Long projectId) {
        return cctvRepository.findByProjectIdAndIsInstalledTrue(projectId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    private CctvCameraDto toDto(CctvCamera camera) {
        return new CctvCameraDto(
            camera.getId(),
            camera.getProject().getId(),
            camera.getCameraName(),
            camera.getLocation(),
            camera.getStreamUrl(),
            camera.getSnapshotUrl(),
            camera.getIsInstalled(),
            camera.getIsActive(),
            camera.getInstallationDate(),
            camera.getLastActive(),
            camera.getCameraType(),
            camera.getResolution(),
            camera.getNotes()
        );
    }
}

