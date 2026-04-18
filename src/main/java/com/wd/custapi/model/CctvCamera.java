package com.wd.custapi.model;

import com.wd.custapi.model.enums.StreamProtocol;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Immutable
@Entity
@Table(name = "cctv_cameras")
@SQLRestriction("deleted_at IS NULL")
public class CctvCamera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "camera_name", nullable = false)
    private String cameraName;

    @Column(name = "location")
    private String location;

    @Column(name = "provider", length = 100)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "stream_protocol", length = 20)
    private StreamProtocol streamProtocol;

    @Column(name = "stream_url", length = 1000)
    private String streamUrl;

    @Column(name = "snapshot_url", length = 1000)
    private String snapshotUrl;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "port")
    private Integer port;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "resolution", length = 50)
    private String resolution;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // All getters (including credentials for server-side URL building)
    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getCameraName() { return cameraName; }
    public String getLocation() { return location; }
    public String getProvider() { return provider; }
    public StreamProtocol getStreamProtocol() { return streamProtocol; }
    public String getStreamUrl() { return streamUrl; }
    public String getSnapshotUrl() { return snapshotUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Integer getPort() { return port; }
    public Boolean getIsActive() { return isActive; }
    public String getResolution() { return resolution; }
    public LocalDate getInstallationDate() { return installationDate; }
    public Integer getDisplayOrder() { return displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
