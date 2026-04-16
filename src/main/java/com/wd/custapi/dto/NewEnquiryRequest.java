package com.wd.custapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewEnquiryRequest(
    @NotBlank(message = "Project type is required")
    String projectType,

    @NotBlank(message = "State is required")
    String state,

    @NotBlank(message = "District is required")
    String district,

    String location,

    String budget,

    String area,

    @Size(max = 2000, message = "Requirements must be under 2000 characters")
    String requirements,

    @Size(max = 2000, message = "Message must be under 2000 characters")
    String message
) {}
