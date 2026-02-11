package com.wd.custapi.model;

import jakarta.persistence.*;

@Entity
@Table(name = "activity_types")
public class ActivityType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(length = 50)
    private String icon;
    
    @Column(length = 20)
    private String color;
    
    // Constructors
    public ActivityType() {}
    
    public ActivityType(String name, String icon, String color) {
        this.name = name;
        this.icon = icon;
        this.color = color;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
}

