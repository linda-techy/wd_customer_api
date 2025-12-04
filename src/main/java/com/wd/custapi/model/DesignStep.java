package com.wd.custapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "design_steps")
public class DesignStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "weight_percentage")
    private Double weightPercentage;

    public DesignStep() {
    }

    public DesignStep(String stepName, Double weightPercentage) {
        this.stepName = stepName;
        this.weightPercentage = weightPercentage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Double getWeightPercentage() {
        return weightPercentage;
    }

    public void setWeightPercentage(Double weightPercentage) {
        this.weightPercentage = weightPercentage;
    }
}
