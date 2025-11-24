package com.wd.custapi.model;

import com.wd.custapi.config.AppConfig;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = AppConfig.REFRESH_TOKEN_TABLE)
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private CustomerUser user;
    
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false)
    private Boolean revoked = false;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public CustomerUser getUser() {
        return user;
    }
    
    public void setUser(CustomerUser user) {
        this.user = user;
    }
    
    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }
    
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
    
    public Boolean getRevoked() {
        return revoked;
    }
    
    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }
}

