package com.linkedin.backend.features.jobs.dto;

import java.util.List;
import java.util.Map;

public record JobRecommendationDto(
        JobResponseDto job,
        double matchScore,
        int matchPercentage,
        String recommendationTier,
        List<String> matchedSkills,
        List<String> missingSkills,
        double candidateExperienceYears,
        Integer requiredMinExperience,
        boolean experienceMatched,
        boolean educationMatched,
        boolean locationMatched,
        String aiReasoning,
        List<String> matchHighlights,
        Map<String, Integer> scoreBreakdown
) {
}
