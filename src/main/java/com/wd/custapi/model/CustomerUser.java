package com.wd.custapi.model;

import com.wd.custapi.config.AppConfig;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = AppConfig.USER_TABLE)
public class CustomerUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Setter(AccessLevel.NONE)
    @Column(name = "lead_source", length = 50, insertable = false, updatable = false)
    private String leadSource;

    @Setter(AccessLevel.NONE)
    @Column(name = "notes", columnDefinition = "TEXT", insertable = false, updatable = false)
    private String notes;

    @Setter(AccessLevel.NONE)
    @Column(name = "customer_type", length = 50, insertable = false, updatable = false)
    private String customerType;

    @Column(name = "fcm_token")
    private String fcmToken;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    @SuppressWarnings("java:S1948") // JPA-managed lazy/eager proxy; entity is never Java-serialized
    private Role role;

    /** Projects this user can access — includes projects where they are the owner
     *  (customer_projects.customer_id) OR a collaborator (project_members join table).
     *  Roles: CUSTOMER (owner), ARCHITECT, INTERIOR_DESIGNER, SITE_ENGINEER, VIEWER. */
    @ManyToMany
    @JoinTable(name = "project_members", joinColumns = @JoinColumn(name = "customer_user_id"), inverseJoinColumns = @JoinColumn(name = "project_id"))
    @SuppressWarnings("java:S1948") // JPA-managed lazy proxy collection; entity is never Java-serialized
    private Set<Project> projects;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "email_verified")
    private Boolean emailVerified = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();

        // Add role authority
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
        }

        // Add permission authorities
        if (role != null && role.getPermissions() != null) {
            authorities.addAll(role.getPermissions().stream()
                    .filter(java.util.Objects::nonNull)
                    .map(permission -> new SimpleGrantedAuthority(permission.getName()))
                    .toList());
        }

        return authorities;
    }

    @Override
    public String getUsername() {
        // Username is the email — delegate to avoid duplicated logic.
        return getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Delegate to the bean getter to avoid duplicated field access.
        // Lombok generates getEnabled() for the Boolean field; UserDetails
        // requires isEnabled(), so this method is kept manual.
        return getEnabled();
    }
}
