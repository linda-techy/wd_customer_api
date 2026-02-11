package com.wd.custapi.model;

import jakarta.persistence.*;

import java.time.LocalDate;

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

    @Column(name = "overall_progress")
    private Double progress = 0.0;

    @Column(name = "project_phase")
    private String projectPhase;

    @Column(name = "design_package")
    private String designPackage;

    @Column(name = "is_design_agreement_signed")
    private Boolean isDesignAgreementSigned = false;

    @Column(name = "sqfeet")
    private Double sqFeet;

    @Column(name = "design_progress")
    private Double designProgress;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

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

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public String getProjectPhase() {
        return projectPhase;
    }

    public void setProjectPhase(String projectPhase) {
        this.projectPhase = projectPhase;
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
}
