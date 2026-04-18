package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CctvService {

    private final CctvCameraRepository cctvRepository;

    public CctvService(CctvCameraRepository cctvRepository) {
        this.cctvRepository = cctvRepository;
    }

    @Transactional(readOnly = true)
    public List<CctvCameraDto> getActiveCameras(Long projectId) {
        return cctvRepository.findByProjectIdAndIsActiveTrueOrderByDisplayOrder(projectId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    private CctvCameraDto toDto(CctvCamera camera) {
        return new CctvCameraDto(
            camera.getId(),
            camera.getProjectId(),
            camera.getCameraName(),
            camera.getLocation(),
            camera.getProvider(),
            camera.getStreamProtocol() != null ? camera.getStreamProtocol().name() : null,
            camera.getStreamUrl(),
            camera.getSnapshotUrl(),
            camera.getIsActive(),
            camera.getInstallationDate(),
            camera.getResolution(),
            camera.getDisplayOrder()
        );
    }
}
