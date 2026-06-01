package com.wd.custapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Getter
@Setter
@SQLDelete(sql = "UPDATE observations SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "observations")
public class Observation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private CustomerUser reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_role_id")
    private StaffRole reportedByRole;

    @Column(name = "reported_date", nullable = false)
    private LocalDateTime reportedDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ObservationStatus status = ObservationStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority priority = Priority.MEDIUM;

    @Column(length = 255)
    private String location;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private CustomerUser resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Enums for status and priority
    public enum ObservationStatus {
        OPEN, IN_PROGRESS, RESOLVED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    // Constructors
    public Observation() {
        // Default constructor required by JPA
    }
}
