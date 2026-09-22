package com.linkedin.backend.features.profile.dto;

import jakarta.validation.constraints.NotEmpty;

public class UserSkillDto {
    private Long id;

    @NotEmpty
    private String skillName;

    public UserSkillDto() {
    }

    public UserSkillDto(Long id, String skillName) {
        this.id = id;
        this.skillName = skillName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }
}
