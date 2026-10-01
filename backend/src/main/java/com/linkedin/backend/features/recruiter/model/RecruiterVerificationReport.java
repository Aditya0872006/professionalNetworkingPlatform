package com.linkedin.backend.features.recruiter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "recruiter_verification_reports")
public class RecruiterVerificationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recruiter_profile_id", unique = true, nullable = false)
    @JsonIgnore
    private RecruiterProfile recruiterProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruiterTrustLevel trustLevel;

    @Column(nullable = false)
    private int trustScore;

    private int emailScore;
    private int websiteScore;
    private int companyScore;
    private int locationScore;
    private int consistencyScore;

    // Pipe-separated flags stored as single TEXT column
    @Column(columnDefinition = "TEXT")
    private String flags;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    // Name of the AI model/approach used (e.g. "huggingface-bart-large-mnli")
    private String modelUsed;

    @CreationTimestamp
    private LocalDateTime analyzedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public RecruiterVerificationReport() {
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public List<String> getFlagList() {
        if (flags == null || flags.isBlank()) return Collections.emptyList();
        return Arrays.asList(flags.split("\\|"));
    }

    public void setFlagList(List<String> flagList) {
        this.flags = flagList == null ? "" : String.join("|", flagList);
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RecruiterProfile getRecruiterProfile() { return recruiterProfile; }
    public void setRecruiterProfile(RecruiterProfile recruiterProfile) { this.recruiterProfile = recruiterProfile; }

    public RecruiterTrustLevel getTrustLevel() { return trustLevel; }
    public void setTrustLevel(RecruiterTrustLevel trustLevel) { this.trustLevel = trustLevel; }

    public int getTrustScore() { return trustScore; }
    public void setTrustScore(int trustScore) { this.trustScore = trustScore; }

    public int getEmailScore() { return emailScore; }
    public void setEmailScore(int emailScore) { this.emailScore = emailScore; }

    public int getWebsiteScore() { return websiteScore; }
    public void setWebsiteScore(int websiteScore) { this.websiteScore = websiteScore; }

    public int getCompanyScore() { return companyScore; }
    public void setCompanyScore(int companyScore) { this.companyScore = companyScore; }

    public int getLocationScore() { return locationScore; }
    public void setLocationScore(int locationScore) { this.locationScore = locationScore; }

    public int getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(int consistencyScore) { this.consistencyScore = consistencyScore; }

    public String getFlags() { return flags; }
    public void setFlags(String flags) { this.flags = flags; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}
