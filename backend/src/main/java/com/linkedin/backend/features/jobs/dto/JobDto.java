package com.linkedin.backend.features.jobs.dto;

import com.linkedin.backend.features.jobs.model.EmploymentType;
import com.linkedin.backend.features.jobs.model.WorkMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record JobDto(
        @NotBlank(message = "Job title is mandatory")
        String title,

        @NotBlank(message = "Company is mandatory")
        String company,

        @NotBlank(message = "Description is mandatory")
        String description,

        @NotBlank(message = "Location is mandatory")
        String location,

        @NotNull(message = "Employment type is mandatory")
        EmploymentType employmentType,

        @NotNull(message = "Work mode is mandatory")
        WorkMode workMode,

        String salary,
        Integer minExperience,
        Integer maxExperience,
        List<String> requiredSkills,
        String educationRequirement,
        LocalDateTime deadline,
        Integer numberOfOpenings
) {
}
