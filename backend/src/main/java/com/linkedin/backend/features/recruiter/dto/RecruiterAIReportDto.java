package com.linkedin.backend.features.recruiter.dto;

import com.linkedin.backend.features.recruiter.model.RecruiterTrustLevel;
import com.linkedin.backend.features.recruiter.model.RecruiterVerificationReport;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO returned to the admin for the AI trust analysis of a recruiter profile.
 * Flattens RecruiterVerificationReport into a clean JSON-serialisable shape.
 */
public class RecruiterAIReportDto {

    private Long recruiterProfileId;
    private RecruiterTrustLevel trustLevel;
    private int trustScore;
    private int emailScore;
    private int websiteScore;
    private int companyScore;
    private int locationScore;
    private int consistencyScore;
    private List<String> flags;
    private String reasoning;
    private String modelUsed;
    private LocalDateTime analyzedAt;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static RecruiterAIReportDto from(RecruiterVerificationReport r) {
        RecruiterAIReportDto dto = new RecruiterAIReportDto();
        dto.recruiterProfileId = r.getRecruiterProfile().getId();
        dto.trustLevel        = r.getTrustLevel();
        dto.trustScore        = r.getTrustScore();
        dto.emailScore        = r.getEmailScore();
        dto.websiteScore      = r.getWebsiteScore();
        dto.companyScore      = r.getCompanyScore();
        dto.locationScore     = r.getLocationScore();
        dto.consistencyScore  = r.getConsistencyScore();
        dto.flags             = r.getFlagList();
        dto.reasoning         = r.getReasoning();
        dto.modelUsed         = r.getModelUsed();
        dto.analyzedAt        = r.getAnalyzedAt();
        return dto;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getRecruiterProfileId() { return recruiterProfileId; }
    public RecruiterTrustLevel getTrustLevel() { return trustLevel; }
    public int getTrustScore() { return trustScore; }
    public int getEmailScore() { return emailScore; }
    public int getWebsiteScore() { return websiteScore; }
    public int getCompanyScore() { return companyScore; }
    public int getLocationScore() { return locationScore; }
    public int getConsistencyScore() { return consistencyScore; }
    public List<String> getFlags() { return flags; }
    public String getReasoning() { return reasoning; }
    public String getModelUsed() { return modelUsed; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
}
