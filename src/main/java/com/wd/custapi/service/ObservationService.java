package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.model.Observation.ObservationStatus;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ObservationService {

        private final ObservationRepository observationRepository;
        private final ProjectRepository projectRepository;
        private final CustomerUserRepository userRepository;
        private final StaffRoleRepository staffRoleRepository;
        private final FileStorageService fileStorageService;
        private final ActivityFeedService activityFeedService;

        public ObservationService(ObservationRepository observationRepository,
                        ProjectRepository projectRepository,
                        CustomerUserRepository userRepository,
                        StaffRoleRepository staffRoleRepository,
                        FileStorageService fileStorageService,
                        ActivityFeedService activityFeedService) {
                this.observationRepository = observationRepository;
                this.projectRepository = projectRepository;
                this.userRepository = userRepository;
                this.staffRoleRepository = staffRoleRepository;
                this.fileStorageService = fileStorageService;
                this.activityFeedService = activityFeedService;
        }

        @Transactional
        public ObservationDto createObservation(Long projectId, ObservationRequest request,
                        MultipartFile image, Long userId) {
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Project not found"));

                CustomerUser user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Observation observation = new Observation();
                observation.setProject(project);
                observation.setTitle(request.title());
                observation.setDescription(request.description());
                observation.setReportedBy(user);
                observation.setPriority(Observation.Priority.valueOf(request.priority()));
                observation.setLocation(request.location());

                if (request.reportedByRoleId() != null) {
                        StaffRole role = staffRoleRepository.findById(request.reportedByRoleId())
                                        .orElseThrow(() -> new RuntimeException("Staff role not found"));
                        observation.setReportedByRole(role);
                }

                if (image != null && !image.isEmpty()) {
                        String imagePath = fileStorageService.storeFile(image,
                                        "projects/" + projectId + "/observations");
                        observation.setImagePath(imagePath);
                }

                observation = observationRepository.save(observation);

                // Create activity feed
                activityFeedService.createActivity(projectId, "OBSERVATION_ADDED",
                                "Observation added: " + request.title(), observation.getId(), userId);

                return toDto(observation);
        }

        @Transactional
        public ObservationDto resolveObservation(Long observationId, ObservationResolveRequest request, Long userId) {
                Observation observation = observationRepository.findById(observationId)
                                .orElseThrow(() -> new RuntimeException("Observation not found"));

                CustomerUser resolvedBy = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                observation.setStatus(ObservationStatus.RESOLVED);
                observation.setResolvedDate(LocalDateTime.now());
                observation.setResolvedBy(resolvedBy);
                observation.setResolutionNotes(request.resolutionNotes());

                observation = observationRepository.save(observation);

                // Create activity feed
                activityFeedService.createActivity(observation.getProject().getId(), "OBSERVATION_RESOLVED",
                                "Observation resolved: " + observation.getTitle(), observation.getId(), userId);

                return toDto(observation);
        }

        public List<ObservationDto> getObservations(Long projectId, String status) {
                List<Observation> observations;
                if (status != null) {
                        ObservationStatus obsStatus = ObservationStatus.valueOf(status);
                        observations = observationRepository
                                        .findByProjectIdAndStatusOrderByPriorityDescReportedDateDesc(projectId,
                                                        obsStatus);
                } else {
                        observations = observationRepository.findByProjectIdOrderByReportedDateDesc(projectId);
                }
                return observations.stream().map(this::toDto).collect(Collectors.toList());
        }

        /**
         * Get active (OPEN, IN_PROGRESS) observations for the project.
         */
        public List<ObservationDto> getActiveObservations(Long projectId) {
                List<ObservationStatus> activeStatuses = List.of(
                                ObservationStatus.OPEN,
                                ObservationStatus.IN_PROGRESS);
                return observationRepository
                                .findByProjectIdAndStatusInOrderByPriorityDescReportedDateDesc(projectId,
                                                activeStatuses)
                                .stream()
                                .map(this::toDto)
                                .collect(Collectors.toList());
        }

        /**
         * Get resolved observations for the project.
         */
        public List<ObservationDto> getResolvedObservations(Long projectId) {
                return observationRepository.findByProjectIdAndStatusOrderByPriorityDescReportedDateDesc(
                                projectId, ObservationStatus.RESOLVED)
                                .stream()
                                .map(this::toDto)
                                .collect(Collectors.toList());
        }

        /**
         * Get observation counts by status.
         */
        public java.util.Map<String, Long> getObservationCounts(Long projectId) {
                java.util.Map<String, Long> counts = new java.util.HashMap<>();
                List<Observation> all = observationRepository.findByProjectIdOrderByReportedDateDesc(projectId);
                counts.put("total", (long) all.size());
                counts.put("active", all.stream()
                                .filter(o -> o.getStatus() != ObservationStatus.RESOLVED)
                                .count());
                counts.put("resolved", all.stream()
                                .filter(o -> o.getStatus() == ObservationStatus.RESOLVED)
                                .count());
                return counts;
        }

        private ObservationDto toDto(Observation obs) {
                return new ObservationDto(
                                obs.getId(),
                                obs.getProject().getId(),
                                obs.getTitle(),
                                obs.getDescription(),
                                obs.getReportedBy().getId(),
                                obs.getReportedBy().getFirstName() + " " + obs.getReportedBy().getLastName(),
                                obs.getReportedByRole() != null ? obs.getReportedByRole().getId() : null,
                                obs.getReportedByRole() != null ? obs.getReportedByRole().getName() : null,
                                obs.getReportedDate(),
                                obs.getStatus().name(),
                                obs.getPriority().name(),
                                obs.getLocation(),
                                obs.getImagePath(),
                                obs.getResolvedDate(),
                                obs.getResolvedBy() != null ? obs.getResolvedBy().getId() : null,
                                obs.getResolvedBy() != null
                                                ? obs.getResolvedBy().getFirstName() + " "
                                                                + obs.getResolvedBy().getLastName()
                                                : null,
                                obs.getResolutionNotes());
        }
}
