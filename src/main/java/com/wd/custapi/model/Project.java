package com.wd.custapi.model;

import com.wd.custapi.model.enums.ProjectPhase;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
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

    // Boolean accessor kept manual (Lombok naming for Boolean is ambiguous)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
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

    // The following fields are read-only mirrors with getters only.
    @Setter(AccessLevel.NONE)
    @Column(name = "project_status")
    private String projectStatus;

    @Setter(AccessLevel.NONE)
    @Column(name = "budget", precision = 15, scale = 2)
    private java.math.BigDecimal budget;

    @Setter(AccessLevel.NONE)
    @Column(name = "contract_type")
    private String contractType;

    @Setter(AccessLevel.NONE)
    @Column(name = "permit_status")
    private String permitStatus;

    @Setter(AccessLevel.NONE)
    @Column(name = "plot_area", precision = 10, scale = 2)
    private java.math.BigDecimal plotArea;

    @Setter(AccessLevel.NONE)
    @Column(name = "floors")
    private Integer floors;

    @Setter(AccessLevel.NONE)
    @Column(name = "facing")
    private String facing;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Owner customer (mirrors Portal API's customer_projects.customer_id).
    // Several native repository queries join via p.customer_id, so the column
    // must exist on this entity's create-drop schema.
    @Column(name = "customer_id")
    private Long customerId;

    @Setter(AccessLevel.NONE)
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // Many-to-many is owned by CustomerUser via project_members
    @ManyToMany(mappedBy = "projects")
    private java.util.Set<CustomerUser> customers;

    // Boolean accessors kept manual (see field annotation above)
    public Boolean getIsDesignAgreementSigned() {
        return isDesignAgreementSigned;
    }

    public void setIsDesignAgreementSigned(Boolean isDesignAgreementSigned) {
        this.isDesignAgreementSigned = isDesignAgreementSigned;
    }

    // Computed accessor — kept manual
    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
}
