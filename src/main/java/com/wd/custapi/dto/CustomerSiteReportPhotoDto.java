package com.wd.custapi.dto;

import com.wd.custapi.model.SiteReportPhoto;

/**
 * Customer-safe DTO for Site Report Photos.
 * Only includes photo URL, excludes internal metadata.
 */
public class CustomerSiteReportPhotoDto {

    private Long id;
    private String photoUrl;

    public CustomerSiteReportPhotoDto(SiteReportPhoto photo) {
        this.id = photo.getId();
        this.photoUrl = photo.getPhotoUrl();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
