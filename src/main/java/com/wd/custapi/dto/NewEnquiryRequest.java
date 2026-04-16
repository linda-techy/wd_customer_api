package com.wd.custapi.dto;

public record NewEnquiryRequest(
    String projectType,
    String state,
    String district,
    String location,
    String budget,
    String area,
    String requirements,
    String message
) {}
