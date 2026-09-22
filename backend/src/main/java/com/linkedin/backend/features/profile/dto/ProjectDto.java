package com.linkedin.backend.features.profile.dto;

import jakarta.validation.constraints.NotEmpty;

public class ProjectDto {
    private Long id;

    @NotEmpty
    private String projectName;

    private String description;
    private String projectUrl;

    public ProjectDto() {
    }

    public ProjectDto(Long id, String projectName, String description, String projectUrl) {
        this.id = id;
        this.projectName = projectName;
        this.description = description;
        this.projectUrl = projectUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }
}
