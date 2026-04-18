package com.wd.custapi.dto;

import jakarta.validation.constraints.NotBlank;

public class SupportTicketReplyRequest {

    @NotBlank
    private String message;

    private String attachmentUrl;

    // Getters and Setters

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
}
