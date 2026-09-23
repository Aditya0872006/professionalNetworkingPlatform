package com.linkedin.backend.features.jobs.dto;

import com.linkedin.backend.features.jobs.model.EmploymentType;
import com.linkedin.backend.features.jobs.model.JobStatus;
import com.linkedin.backend.features.jobs.model.WorkMode;

import java.time.LocalDateTime;
import java.util.List;

public record JobResponseDto(
        Long id,
        Long recruiterId,
        String recruiterName,
        String title,
        String company,
        String description,
        String location,
        EmploymentType employmentType,
        WorkMode workMode,
        String salary,
        Integer minExperience,
        Integer maxExperience,
        List<String> requiredSkills,
        String educationRequirement,
        LocalDateTime deadline,
        Integer numberOfOpenings,
        JobStatus status,
        long applicantCount,
        boolean isExpired,
        boolean hasApplied,
        LocalDateTime createdAt
) {
}
