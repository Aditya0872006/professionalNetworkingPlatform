package com.linkedin.backend.features.jobs.dto;

import java.util.List;

public record TopApplicantEvaluationDto(
        boolean isTopApplicant,
        double matchScore,
        int matchPercentage,
        Integer rank,
        int totalApplicants,
        Integer percentile,
        boolean isEarlyApplicant,
        List<String> matchedSkills,
        List<String> missingSkills,
        String headlineMessage,
        String matchTier
) {
}
