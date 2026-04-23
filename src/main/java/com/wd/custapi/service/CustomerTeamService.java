package com.wd.custapi.service;

import com.wd.custapi.dto.TeamContactDto;
import com.wd.custapi.model.ProjectMember;
import com.wd.custapi.repository.ProjectMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CustomerTeamService {

    private static final List<String> ALLOWED_ROLES = List.of(
            "PROJECT_MANAGER", "SITE_ENGINEER", "ARCHITECT", "INTERIOR_DESIGNER");

    private static final Map<String, String> DESIGNATION_BY_ROLE = Map.of(
            "PROJECT_MANAGER", "Project Manager",
            "SITE_ENGINEER", "Site Engineer",
            "ARCHITECT", "Architect",
            "INTERIOR_DESIGNER", "Interior Designer");

    private final ProjectMemberRepository repository;
    private final PortalUserLookup userLookup;

    public CustomerTeamService(ProjectMemberRepository repository, PortalUserLookup userLookup) {
        this.repository = repository;
        this.userLookup = userLookup;
    }

    @Transactional(readOnly = true)
    public List<TeamContactDto> getTeamForProject(Long projectId) {
        return repository.findVisibleStaffByProject(projectId, ALLOWED_ROLES).stream()
                .map(this::toDto)
                .toList();
    }

    private TeamContactDto toDto(ProjectMember m) {
        TeamContactDto dto = new TeamContactDto();
        dto.setUserId(m.getPortalUserId());
        dto.setRole(m.getRoleInProject());
        dto.setDesignation(DESIGNATION_BY_ROLE.getOrDefault(m.getRoleInProject(), m.getRoleInProject()));

        PortalUserLookup.View view = userLookup.lookup(m.getPortalUserId());
        if (view != null) {
            dto.setName(view.name());
            dto.setPhotoUrl(view.photoUrl());
            if (Boolean.TRUE.equals(m.getShareWithCustomer())) {
                dto.setPhone(view.phone());
                dto.setEmail(view.email());
            }
        }
        return dto;
    }
}
