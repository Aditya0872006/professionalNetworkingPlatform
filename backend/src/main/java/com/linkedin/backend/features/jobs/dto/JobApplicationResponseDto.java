package com.linkedin.backend.features.jobs.dto;

import com.linkedin.backend.features.jobs.model.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record JobApplicationResponseDto(
        Long id,
        Long jobId,
        String jobTitle,
        String company,
        Long applicantId,
        String applicantName,
        String applicantEmail,
        String applicantPosition,
        String applicantLocation,
        String applicantProfilePicture,
        List<String> applicantSkills,
        String resumeUrl,
        ApplicationStatus status,
        LocalDateTime appliedAt
) {
}
