package com.linkedin.backend.features.resume.dto;

public class ResumeGenerationResponseDto {

    private String texFileUrl;
    private String pdfFileUrl;
    private String status;
    private String message;

    public ResumeGenerationResponseDto(String texFileUrl, String pdfFileUrl, String status, String message) {
        this.texFileUrl = texFileUrl;
        this.pdfFileUrl = pdfFileUrl;
        this.status = status;
        this.message = message;
    }

    public String getTexFileUrl() {
        return texFileUrl;
    }

    public void setTexFileUrl(String texFileUrl) {
        this.texFileUrl = texFileUrl;
    }

    public String getPdfFileUrl() {
        return pdfFileUrl;
    }

    public void setPdfFileUrl(String pdfFileUrl) {
        this.pdfFileUrl = pdfFileUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
