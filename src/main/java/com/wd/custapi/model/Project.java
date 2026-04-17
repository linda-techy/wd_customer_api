package com.wd.custapi.model;

import com.wd.custapi.model.enums.ProjectPhase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@SQLDelete(sql = "UPDATE customer_projects SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "customer_projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_uuid", nullable = false, unique = true)
    private java.util.UUID projectUuid = java.util.UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    private String code;

    private String location;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(name = "overall_progress", precision = 5, scale = 2)
    private BigDecimal progress = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_phase")
    private ProjectPhase projectPhase;

    @Column(name = "project_type", length = 255)
    private String projectType;

    @Column(name = "design_package")
    private String designPackage;

    @Column(name = "is_design_agreement_signed")
    private Boolean isDesignAgreementSigned = false;

    @Column(name = "sqfeet")
    @JdbcTypeCode(SqlTypes.NUMERIC)
    private Double sqFeet;

    @Column(name = "design_progress")
    private Double designProgress;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "project_status")
    private String projectStatus;

    @Column(name = "budget", precision = 15, scale = 2)
    private java.math.BigDecimal budget;

    @Column(name = "contract_type")
    private String contractType;

    @Column(name = "permit_status")
    private String permitStatus;

    @Column(name = "plot_area", precision = 10, scale = 2)
    private java.math.BigDecimal plotArea;

    @Column(name = "floors")
    private Integer floors;

    @Column(name = "facing")
    private String facing;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // Many-to-many is owned by CustomerUser via project_members
    @ManyToMany(mappedBy = "projects")
    private java.util.Set<CustomerUser> customers;

    public java.util.Set<CustomerUser> getCustomers() {
        return customers;
    }

    public void setCustomers(java.util.Set<CustomerUser> customers) {
        this.customers = customers;
    }

    // Minimal getters/setters for DTO mapping
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public java.util.UUID getProjectUuid() {
        return projectUuid;
    }

    public void setProjectUuid(java.util.UUID projectUuid) {
        this.projectUuid = projectUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getProgress() {
        return progress;
    }

    public void setProgress(BigDecimal progress) {
        this.progress = progress;
    }

    public ProjectPhase getProjectPhase() {
        return projectPhase;
    }

    public void setProjectPhase(ProjectPhase projectPhase) {
        this.projectPhase = projectPhase;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getDesignPackage() {
        return designPackage;
    }

    public void setDesignPackage(String designPackage) {
        this.designPackage = designPackage;
    }

    public Boolean getIsDesignAgreementSigned() {
        return isDesignAgreementSigned;
    }

    public void setIsDesignAgreementSigned(Boolean isDesignAgreementSigned) {
        this.isDesignAgreementSigned = isDesignAgreementSigned;
    }

    public Double getSqFeet() {
        return sqFeet;
    }

    public void setSqFeet(Double sqFeet) {
        this.sqFeet = sqFeet;
    }

    public Double getDesignProgress() {
        return designProgress;
    }

    public void setDesignProgress(Double progress) {
        this.designProgress = progress;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public java.math.BigDecimal getBudget() {
        return budget;
    }

    public String getContractType() {
        return contractType;
    }

    public String getPermitStatus() {
        return permitStatus;
    }

    public java.math.BigDecimal getPlotArea() {
        return plotArea;
    }

    public Integer getFloors() {
        return floors;
    }

    public String getFacing() {
        return facing;
    }

    public Long getVersion() { return version; }
}
