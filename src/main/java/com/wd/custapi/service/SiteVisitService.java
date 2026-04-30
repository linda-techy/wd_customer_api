package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import com.wd.custapi.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SiteVisitService {

    private static final Logger logger = LoggerFactory.getLogger(SiteVisitService.class);

    /**
     * Service for managing site visits with GPS-based proximity validation.
     */
    private final SiteVisitRepository siteVisitRepository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final ActivityFeedService activityFeedService;
    private final PortalUserLookup portalUserLookup;

    public SiteVisitService(SiteVisitRepository siteVisitRepository,
            ProjectRepository projectRepository,
            CustomerUserRepository userRepository,
            StaffRoleRepository staffRoleRepository,
            ActivityFeedService activityFeedService,
            PortalUserLookup portalUserLookup) {
        this.siteVisitRepository = siteVisitRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.activityFeedService = activityFeedService;
        this.portalUserLookup = portalUserLookup;
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

        // Validate GPS coordinates are provided
        if (request.latitude() == null || request.longitude() == null) {
            throw new RuntimeException("GPS coordinates are required for check-in. Please enable location services.");
        }

        // Validate GPS proximity - must be within 2km of project site
        Double distanceKm = null;
        if (project.hasLocation()) {
            distanceKm = GeoUtils.calculateDistanceKm(
                    request.latitude(), request.longitude(),
                    project.getLatitude(), project.getLongitude());

            if (!GeoUtils.isWithinRadius(
                    request.latitude(), request.longitude(),
                    project.getLatitude(), project.getLongitude(),
                    GeoUtils.MAX_CHECKIN_DISTANCE_KM)) {
                throw new RuntimeException(
                        "Check-in failed: You are " + GeoUtils.formatDistance(distanceKm) +
                        " away from the project site. You must be within " +
                        GeoUtils.formatDistance(GeoUtils.MAX_CHECKIN_DISTANCE_KM) +
                        " to check in.");
            }
            logger.info("Check-in GPS validated: user is {} from project site", GeoUtils.formatDistance(distanceKm));
        }

        SiteVisit visit = new SiteVisit();
        visit.setProject(project);
        visit.setVisitor(visitor);
        visit.setCheckInTime(LocalDateTime.now());
        visit.setPurpose(request.purpose());
        visit.setLocation(request.location());
        visit.setWeatherConditions(request.weatherConditions());
        visit.setCheckInLatitude(request.latitude());
        visit.setCheckInLongitude(request.longitude());
        if (distanceKm != null) {
            visit.setDistanceFromProjectCheckIn(Math.round(distanceKm * 1000.0) / 1000.0); // Round to 3 decimals
        }

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

        // Validate GPS coordinates are provided
        if (request.latitude() == null || request.longitude() == null) {
            throw new RuntimeException("GPS coordinates are required for check-out. Please enable location services.");
        }

        // Validate GPS proximity - must be within 2km of project site
        Project project = visit.getProject();
        Double distanceKm = null;
        if (project.hasLocation()) {
            distanceKm = GeoUtils.calculateDistanceKm(
                    request.latitude(), request.longitude(),
                    project.getLatitude(), project.getLongitude());

            if (!GeoUtils.isWithinRadius(
                    request.latitude(), request.longitude(),
                    project.getLatitude(), project.getLongitude(),
                    GeoUtils.MAX_CHECKIN_DISTANCE_KM)) {
                throw new RuntimeException(
                        "Check-out failed: You are " + GeoUtils.formatDistance(distanceKm) +
                        " away from the project site. You must be within " +
                        GeoUtils.formatDistance(GeoUtils.MAX_CHECKIN_DISTANCE_KM) +
                        " to check out.");
            }
            logger.info("Check-out GPS validated: user is {} from project site", GeoUtils.formatDistance(distanceKm));
        }

        visit.setCheckOutTime(LocalDateTime.now());
        visit.setNotes(request.notes());
        visit.setFindings(request.findings());
        visit.setCheckOutLatitude(request.latitude());
        visit.setCheckOutLongitude(request.longitude());
        if (distanceKm != null) {
            visit.setDistanceFromProjectCheckOut(Math.round(distanceKm * 1000.0) / 1000.0);
        }

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

    /**
     * Map {@link SiteVisit} → DTO. Handles two row origins:
     *
     * <ul>
     *   <li><b>Customer-side:</b> {@code visitor} (CustomerUser) populated.
     *       Use the customer's name; role from {@code visitorRole}.</li>
     *   <li><b>Portal-side (staff):</b> {@code visitor} is null but
     *       {@code visitedBy} (portal_user_id) is set. Look up the staff
     *       name via {@link PortalUserLookup}; use the portal-side
     *       {@code visit_type} (SITE_ENGINEER → "Site Engineer") as the
     *       customer-facing role label.</li>
     * </ul>
     *
     * <p>Both branches must be NPE-safe — the Customer Flutter app crashes
     * on a null visitorName, so we fall back to "Staff" when no lookup
     * resolves.
     */
    private SiteVisitDto toDto(SiteVisit visit) {
        Long visitorId;
        String visitorName;
        Long visitorRoleId;
        String visitorRoleName;

        if (visit.getVisitor() != null) {
            visitorId = visit.getVisitor().getId();
            visitorName = (safe(visit.getVisitor().getFirstName())
                    + " " + safe(visit.getVisitor().getLastName())).trim();
            visitorRoleId = visit.getVisitorRole() != null ? visit.getVisitorRole().getId() : null;
            visitorRoleName = visit.getVisitorRole() != null ? visit.getVisitorRole().getName() : null;
        } else if (visit.getVisitedBy() != null) {
            // Staff-side row from the Portal API. Resolve the staff member's
            // name via the read-only portal_users lookup; fall back to a
            // generic label if the lookup misses (e.g. user deleted).
            PortalUserLookup.View staff = portalUserLookup.lookup(visit.getVisitedBy());
            visitorId = visit.getVisitedBy();
            visitorName = (staff != null && staff.name() != null && !staff.name().isBlank())
                    ? staff.name()
                    : "Staff";
            visitorRoleId = null;
            visitorRoleName = humaniseVisitType(visit.getVisitType());
        } else {
            // Defensive — shouldn't happen, both pointers null. Keep the
            // row visible so the customer doesn't see a silent gap. 0L is
            // a placeholder because the customer Flutter model declares
            // visitorId as a non-nullable int.
            visitorId = 0L;
            visitorName = "Staff";
            visitorRoleId = null;
            visitorRoleName = humaniseVisitType(visit.getVisitType());
        }

        return new SiteVisitDto(
                visit.getId(),
                visit.getProject().getId(),
                visitorId,
                visitorName,
                visitorRoleId,
                visitorRoleName,
                visit.getCheckInTime(),
                visit.getCheckOutTime(),
                visit.getPurpose(),
                visit.getNotes(),
                visit.getFindings(),
                visit.getLocation(),
                visit.getWeatherConditions(),
                visit.getAttendees() != null ? Arrays.asList(visit.getAttendees()) : null,
                visit.getCheckInLatitude(),
                visit.getCheckInLongitude(),
                visit.getCheckOutLatitude(),
                visit.getCheckOutLongitude(),
                visit.getDistanceFromProjectCheckIn(),
                visit.getDistanceFromProjectCheckOut());
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** Convert an enum-shaped visit type ("SITE_ENGINEER") to a customer-facing label ("Site Engineer"). */
    private static String humaniseVisitType(String visitType) {
        if (visitType == null || visitType.isBlank()) return null;
        StringBuilder out = new StringBuilder();
        boolean upper = true;
        for (char c : visitType.toCharArray()) {
            if (c == '_') { out.append(' '); upper = true; continue; }
            out.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
            upper = false;
        }
        return out.toString();
    }
}
