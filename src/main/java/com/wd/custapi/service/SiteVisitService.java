package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SiteVisitService {

    /**
     * Service for managing site visits.
     */
    private final SiteVisitRepository siteVisitRepository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final ActivityFeedService activityFeedService;

    public SiteVisitService(SiteVisitRepository siteVisitRepository,
            ProjectRepository projectRepository,
            CustomerUserRepository userRepository,
            StaffRoleRepository staffRoleRepository,
            ActivityFeedService activityFeedService) {
        this.siteVisitRepository = siteVisitRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.activityFeedService = activityFeedService;
    }

    @Transactional
    public SiteVisitDto checkIn(Long projectId, SiteVisitCheckInRequest request, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        CustomerUser visitor = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if there's an active visit (no checkout)
        siteVisitRepository.findTopByProjectIdAndVisitorIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(projectId, userId)
                .ifPresent(activeVisit -> {
                    throw new RuntimeException("Please check out from your current visit first");
                });

        SiteVisit visit = new SiteVisit();
        visit.setProject(project);
        visit.setVisitor(visitor);
        visit.setCheckInTime(LocalDateTime.now());
        visit.setPurpose(request.purpose());
        visit.setLocation(request.location());
        visit.setWeatherConditions(request.weatherConditions());

        if (request.visitorRoleId() != null) {
            StaffRole role = staffRoleRepository.findById(request.visitorRoleId())
                    .orElseThrow(() -> new RuntimeException("Staff role not found"));
            visit.setVisitorRole(role);
        }

        if (request.attendees() != null) {
            visit.setAttendees(request.attendees().toArray(new String[0]));
        }

        visit = siteVisitRepository.save(visit);

        // Create activity feed
        activityFeedService.createActivity(projectId, "SITE_VISIT_LOGGED",
                "Site visit started", visit.getId(), userId);

        return toDto(visit);
    }

    @Transactional
    public SiteVisitDto checkOut(Long visitId, SiteVisitCheckOutRequest request) {
        SiteVisit visit = siteVisitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Site visit not found"));

        if (visit.getCheckOutTime() != null) {
            throw new RuntimeException("Already checked out");
        }

        visit.setCheckOutTime(LocalDateTime.now());
        visit.setNotes(request.notes());
        visit.setFindings(request.findings());

        visit = siteVisitRepository.save(visit);
        return toDto(visit);
    }

    public List<SiteVisitDto> getProjectVisits(Long projectId) {
        return siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(projectId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get completed visits (with checkout time).
     */
    public List<SiteVisitDto> getCompletedVisits(Long projectId) {
        return siteVisitRepository.findByProjectIdAndCheckOutTimeIsNotNullOrderByCheckInTimeDesc(projectId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get upcoming/ongoing visits (without checkout time or scheduled in future).
     */
    public List<SiteVisitDto> getOngoingVisits(Long projectId) {
        return siteVisitRepository.findByProjectIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(projectId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private SiteVisitDto toDto(SiteVisit visit) {
        return new SiteVisitDto(
                visit.getId(),
                visit.getProject().getId(),
                visit.getVisitor().getId(),
                visit.getVisitor().getFirstName() + " " + visit.getVisitor().getLastName(),
                visit.getVisitorRole() != null ? visit.getVisitorRole().getId() : null,
                visit.getVisitorRole() != null ? visit.getVisitorRole().getName() : null,
                visit.getCheckInTime(),
                visit.getCheckOutTime(),
                visit.getPurpose(),
                visit.getNotes(),
                visit.getFindings(),
                visit.getLocation(),
                visit.getWeatherConditions(),
                visit.getAttendees() != null ? Arrays.asList(visit.getAttendees()) : null);
    }
}
