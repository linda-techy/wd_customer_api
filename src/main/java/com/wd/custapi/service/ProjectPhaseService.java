package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.ProjectPhaseDto;
import com.wd.custapi.model.ProjectPhase;
import com.wd.custapi.repository.ProjectPhaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Read-only service exposing construction phase data to customers.
 * Portal API owns all writes; this service only reads.
 */
@Service
@Transactional(readOnly = true)
public class ProjectPhaseService {

    @Autowired
    private ProjectPhaseRepository projectPhaseRepository;

    /**
     * Returns all phases for a project in display order.
     * Used to render the construction timeline on the customer app.
     */
    public List<ProjectPhaseDto> getProjectPhases(Long projectId) {
        return projectPhaseRepository.findByProjectIdOrderByDisplayOrder(projectId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ProjectPhaseDto toDto(ProjectPhase phase) {
        return new ProjectPhaseDto(
                phase.getId(),
                phase.getPhaseName(),
                phase.getStatus(),
                phase.getDisplayOrder(),
                phase.getPlannedStart(),
                phase.getPlannedEnd(),
                phase.getActualStart(),
                phase.getActualEnd()
        );
    }
}
