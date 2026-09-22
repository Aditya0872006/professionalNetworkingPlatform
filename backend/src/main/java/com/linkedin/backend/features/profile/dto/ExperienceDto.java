package com.linkedin.backend.features.profile.dto;

import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;

public class ExperienceDto {
    private Long id;

    @NotEmpty
    private String companyName;

    @NotEmpty
    private String jobTitle;

    private LocalDate startDate;
    private LocalDate endDate;
    private String description;

    public ExperienceDto() {
    }

    public ExperienceDto(Long id, String companyName, String jobTitle, LocalDate startDate, LocalDate endDate, String description) {
        this.id = id;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
