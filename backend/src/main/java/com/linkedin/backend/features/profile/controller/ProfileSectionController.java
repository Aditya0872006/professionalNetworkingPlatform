package com.linkedin.backend.features.profile.controller;

import com.linkedin.backend.dto.Response;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.profile.dto.EducationDto;
import com.linkedin.backend.features.profile.dto.ExperienceDto;
import com.linkedin.backend.features.profile.dto.ProjectDto;
import com.linkedin.backend.features.profile.dto.UserSkillDto;
import com.linkedin.backend.features.profile.service.ProfileSectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileSectionController {

    private final ProfileSectionService profileSectionService;

    public ProfileSectionController(ProfileSectionService profileSectionService) {
        this.profileSectionService = profileSectionService;
    }

    // ─── Skills ─────────────────────────────────────────────────────────────

    @GetMapping("/users/{userId}/skills")
    public ResponseEntity<List<UserSkillDto>> getSkills(@PathVariable Long userId) {
        return ResponseEntity.ok(profileSectionService.getSkills(userId));
    }

    @PostMapping("/skills")
    public ResponseEntity<UserSkillDto> addSkill(
            @RequestAttribute("authenticatedUser") User user,
            @Valid @RequestBody UserSkillDto dto) {
        return ResponseEntity.ok(profileSectionService.addSkill(user, dto));
    }

    @PutMapping("/skills/{skillId}")
    public ResponseEntity<UserSkillDto> updateSkill(
            @RequestAttribute("authenticatedUser") User user,
            @PathVariable Long skillId,
            @Valid @RequestBody UserSkillDto dto) {
        return ResponseEntity.ok(profileSectionService.updateSkill(user, skillId, dto));
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<Response> deleteSkill(
            @RequestAttribute("authenticatedUser") User user,
            @PathVariable Long skillId) {
        profileSectionService.deleteSkill(user, skillId);
        return ResponseEntity.ok(new Response("Skill deleted successfully."));
    }

    // ─── Education ──────────────────────────────────────────────────────────

    @GetMapping("/users/{userId}/education")
    public ResponseEntity<List<EducationDto>> getEducations(@PathVariable Long userId) {
        return ResponseEntity.ok(profileSectionService.getEducations(userId));
    }

    @PostMapping("/education")
    public ResponseEntity<EducationDto> addEducation(
            @RequestAttribute("authenticatedUser") User user,
            @Valid @RequestBody EducationDto dto) {
        return ResponseEntity.ok(profileSectionService.addEducation(user, dto));
    }

    @PutMapping("/education/{educationId}")
    public ResponseEntity<EducationDto> updateEducation(
            @RequestAttribute("authenticatedUser") User user,
            @PathVariable Long educationId,
            @Valid @RequestBody EducationDto dto) {
        return ResponseEntity.ok(profileSectionService.updateEducation(user, educationId, dto));
    }

    @DeleteMapping("/education/{educationId}")
    public ResponseEntity<Response> deleteEducation(
            @RequestAttribute("authenticatedUser") User user,
            @PathVariable Long educationId) {
        profileSectionService.deleteEducation(user, educationId);
        return ResponseEntity.ok(new Response("Education deleted successfully."));
    }

    // ─── Experience ─────────────────────────────────────────────────────────

    @GetMapping("/users/{userId}/experience")
    public ResponseEntity<List<ExperienceDto>> getExperiences(@PathVariable Long userId) {
        return ResponseEntity.ok(profileSectionService.getExperiences(userId));
    }

    @PostMapping("/experience")
    public ResponseEntity<ExperienceDto> addExperience(
            @RequestAttribute("authenticatedUser") User user,
            @Valid @RequestBody ExperienceDto dto) {
        return ResponseEntity.ok(profileSectionService.addExperience(user, dto));
    }

    @PutMapping("/experience/{experienceId}")
    public ResponseEntity<ExperienceDto> updateExperience(
            @RequestAttribute("authenticatedUser") User user,
            @PathVariable Long experienceId,
            @Valid @RequestBody ExperienceDto dto) {
        return ResponseEntity.ok(profileSectionService.updateExperience(user, experienceId, dto));
    }

    @DeleteMapping("/experience/{experienceId}")
    public ResponseEntity<Response> deleteExperience(
            @RequestAttribute("authenticatedUser") User user,
            @PathVariable Long experienceId) {
        profileSectionService.deleteExperience(user, experienceId);
        return ResponseEntity.ok(new Response("Experience deleted successfully."));
    }

    // ─── Project ────────────────────────────────────────────────────────────

    @GetMapping("/users/{userId}/projects")
    public ResponseEntity<List<ProjectDto>> getProjects(@PathVariable Long userId) {
        return ResponseEntity.ok(profileSectionService.getProjects(userId));
    }

    @PostMapping("/projects")
    public ResponseEntity<ProjectDto> addProject(
            @RequestAttribute("authenticatedUser") User user,
            @Valid @RequestBody ProjectDto dto) {
        return ResponseEntity.ok(profileSectionService.addProject(user, dto));
    }

    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ProjectDto> updateProject(
            @RequestAttribute("authenticatedUser") User user,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectDto dto) {
        return ResponseEntity.ok(profileSectionService.updateProject(user, projectId, dto));
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Response> deleteProject(
            @RequestAttribute("authenticatedUser") User user,
            @PathVariable Long projectId) {
        profileSectionService.deleteProject(user, projectId);
        return ResponseEntity.ok(new Response("Project deleted successfully."));
    }
}
