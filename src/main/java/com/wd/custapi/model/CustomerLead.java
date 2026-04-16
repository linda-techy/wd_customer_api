package com.wd.custapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@SQLRestriction("deleted_at IS NULL")
public class CustomerLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lead_id")
    private Long id;

    @Column(insertable = false, updatable = false)
    private String name;

    @Column(insertable = false, updatable = false)
    private String email;

    @Column(insertable = false, updatable = false)
    private String phone;

    @Column(name = "whatsapp_number", insertable = false, updatable = false)
    private String whatsappNumber;

    @Column(name = "project_type", insertable = false, updatable = false)
    private String projectType;

    @Column(name = "project_description", columnDefinition = "TEXT", insertable = false, updatable = false)
    private String projectDescription;

    @Column(columnDefinition = "TEXT", insertable = false, updatable = false)
    private String requirements;

    @Column(precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal budget;

    @Column(name = "project_sqft_area", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal projectSqftArea;

    @Column(insertable = false, updatable = false)
    private String location;

    @Column(insertable = false, updatable = false)
    private String district;

    @Column(insertable = false, updatable = false)
    private String state;

    @Column(name = "lead_status", insertable = false, updatable = false)
    private String leadStatus;

    @Column(name = "lead_source", insertable = false, updatable = false)
    private String leadSource;

    @Column(name = "next_follow_up", insertable = false, updatable = false)
    private LocalDateTime nextFollowUp;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "customer_user_id", insertable = false, updatable = false)
    private Long customerUserId;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;

    // Getters only — no setters, entity is fully read-only
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public String getProjectType() { return projectType; }
    public String getProjectDescription() { return projectDescription; }
    public String getRequirements() { return requirements; }
    public BigDecimal getBudget() { return budget; }
    public BigDecimal getProjectSqftArea() { return projectSqftArea; }
    public String getLocation() { return location; }
    public String getDistrict() { return district; }
    public String getState() { return state; }
    public String getLeadStatus() { return leadStatus; }
    public String getLeadSource() { return leadSource; }
    public LocalDateTime getNextFollowUp() { return nextFollowUp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCustomerUserId() { return customerUserId; }

    public String getCustomerFriendlyStatus() {
        if (leadStatus == null) return "Processing";
        return switch (leadStatus.toLowerCase()) {
            case "new_inquiry" -> "Enquiry Received";
            case "contacted" -> "We've Reached Out";
            case "qualified" -> "Under Review";
            case "proposal_sent" -> "Proposal Sent";
            case "negotiation" -> "In Discussion";
            case "converted" -> "Project Started";
            case "lost" -> "Enquiry Closed";
            default -> "Processing";
        };
    }
}
