package com.wd.custapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "project_design_steps", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "project_id", "step_id" })
})
public class ProjectDesignStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "step_id", nullable = false)
    private DesignStep step;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DesignStepStatus status = DesignStepStatus.NOT_STARTED;

    @Column(name = "progress_percentage")
    private Double progressPercentage = 0.0;

    public ProjectDesignStep() {
    }

    public ProjectDesignStep(Project project, DesignStep step) {
        this.project = project;
        this.step = step;
        this.status = DesignStepStatus.NOT_STARTED;
        this.progressPercentage = 0.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public DesignStep getStep() {
        return step;
    }

    public void setStep(DesignStep step) {
        this.step = step;
    }

    public DesignStepStatus getStatus() {
        return status;
    }

    public void setStatus(DesignStepStatus status) {
        this.status = status;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
