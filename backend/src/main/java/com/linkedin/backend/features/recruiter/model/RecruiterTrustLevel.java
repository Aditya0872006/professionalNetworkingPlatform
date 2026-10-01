package com.linkedin.backend.features.recruiter.model;

public enum RecruiterTrustLevel {
    LEGITIMATE,       // score 80-100 → suggest approve
    SUSPICIOUS,       // score 50-79  → review carefully
    LIKELY_FAKE,      // score 25-49  → warn admin
    HIGH_RISK         // score 0-24   → suggest reject
}
