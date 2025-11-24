package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectDtos;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    public List<ProjectDtos.ProjectCard> getProjectsForCustomerEmail(String email) {
        return projectRepository.findAllByCustomerEmail(email).stream().map(this::toCard).collect(Collectors.toList());
    }

    public long getProjectCountForCustomer(Long customerId) {
        return projectRepository.countByCustomerId(customerId);
    }

    private ProjectDtos.ProjectCard toCard(Project project) {
        ProjectDtos.ProjectCard card = new ProjectDtos.ProjectCard();
        card.id = project.getId();
        card.name = project.getName();
        card.code = project.getCode();
        card.location = project.getLocation();
        card.startDate = project.getStartDate();
        card.endDate = project.getEndDate();
        return card;
    }
}


