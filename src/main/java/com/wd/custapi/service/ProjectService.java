package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectDtos;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final com.wd.custapi.repository.DesignStepRepository designStepRepository;
    private final com.wd.custapi.repository.ProjectDesignStepRepository projectDesignStepRepository;

    public ProjectService(ProjectRepository projectRepository,
            com.wd.custapi.repository.DesignStepRepository designStepRepository,
            com.wd.custapi.repository.ProjectDesignStepRepository projectDesignStepRepository) {
        this.projectRepository = projectRepository;
        this.designStepRepository = designStepRepository;
        this.projectDesignStepRepository = projectDesignStepRepository;
    }

    public List<ProjectDtos.ProjectCard> getProjectsForCustomerEmail(String email) {
        return projectRepository.findAllByCustomerEmail(email).stream().map(this::toCard).toList();
    }

    public long getProjectCountForCustomer(Long customerId) {
        return projectRepository.countByCustomerId(customerId);
    }

    @org.springframework.transaction.annotation.Transactional
    public void initializeProjectDesignSteps(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        List<com.wd.custapi.model.ProjectDesignStep> steps = designStepRepository.findAll().stream()
                .map(step -> new com.wd.custapi.model.ProjectDesignStep(project, step))
                .toList();
        projectDesignStepRepository.saveAll(steps);
    }

    private ProjectDtos.ProjectCard toCard(Project project) {
        ProjectDtos.ProjectCard card = new ProjectDtos.ProjectCard();
        card.setId(project.getId());
        card.setName(project.getName());
        card.setCode(project.getCode());
        card.setLocation(project.getLocation());
        card.setStartDate(project.getStartDate());
        card.setEndDate(project.getEndDate());
        return card;
    }
}
