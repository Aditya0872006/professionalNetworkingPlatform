package com.linkedin.backend.features.profile.service;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.authentication.repository.UserRepository;
import com.linkedin.backend.features.profile.dto.EducationDto;
import com.linkedin.backend.features.profile.dto.ExperienceDto;
import com.linkedin.backend.features.profile.dto.ProjectDto;
import com.linkedin.backend.features.profile.dto.UserSkillDto;
import com.linkedin.backend.features.profile.model.Education;
import com.linkedin.backend.features.profile.model.Experience;
import com.linkedin.backend.features.profile.model.Project;
import com.linkedin.backend.features.profile.model.UserSkill;
import com.linkedin.backend.features.profile.repository.EducationRepository;
import com.linkedin.backend.features.profile.repository.ExperienceRepository;
import com.linkedin.backend.features.profile.repository.ProjectRepository;
import com.linkedin.backend.features.profile.repository.UserSkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProfileSectionService {

    private final UserSkillRepository skillRepository;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProfileSectionService(
            UserSkillRepository skillRepository,
            EducationRepository educationRepository,
            ExperienceRepository experienceRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        this.skillRepository = skillRepository;
        this.educationRepository = educationRepository;
        this.experienceRepository = experienceRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // ─── Skills ─────────────────────────────────────────────────────────────

    public List<UserSkillDto> getSkills(Long userId) {
        User user = getUser(userId);
        return skillRepository.findByUserOrderByIdAsc(user)
                .stream()
                .map(s -> new UserSkillDto(s.getId(), s.getSkillName()))
                .toList();
    }

    public UserSkillDto addSkill(User user, UserSkillDto dto) {
        UserSkill skill = new UserSkill(user, dto.getSkillName().trim());
        UserSkill saved = skillRepository.save(skill);
        return new UserSkillDto(saved.getId(), saved.getSkillName());
    }

    public UserSkillDto updateSkill(User user, Long skillId, UserSkillDto dto) {
        UserSkill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found"));
        if (!skill.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this skill");
        }
        skill.setSkillName(dto.getSkillName().trim());
        UserSkill saved = skillRepository.save(skill);
        return new UserSkillDto(saved.getId(), saved.getSkillName());
    }

    public void deleteSkill(User user, Long skillId) {
        UserSkill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found"));
        if (!skill.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this skill");
        }
        skillRepository.delete(skill);
    }

    // ─── Education ──────────────────────────────────────────────────────────

    public List<EducationDto> getEducations(Long userId) {
        User user = getUser(userId);
        return educationRepository.findByUserOrderByStartDateDesc(user)
                .stream()
                .map(e -> new EducationDto(
                        e.getId(),
                        e.getInstitution(),
                        e.getDegree(),
                        e.getFieldOfStudy(),
                        e.getStartDate(),
                        e.getEndDate()
                ))
                .toList();
    }

    public EducationDto addEducation(User user, EducationDto dto) {
        Education education = new Education(
                user,
                dto.getInstitution().trim(),
                dto.getDegree() != null ? dto.getDegree().trim() : null,
                dto.getFieldOfStudy() != null ? dto.getFieldOfStudy().trim() : null,
                dto.getStartDate(),
                dto.getEndDate()
        );
        Education saved = educationRepository.save(education);
        return new EducationDto(
                saved.getId(),
                saved.getInstitution(),
                saved.getDegree(),
                saved.getFieldOfStudy(),
                saved.getStartDate(),
                saved.getEndDate()
        );
    }

    public EducationDto updateEducation(User user, Long educationId, EducationDto dto) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Education not found"));
        if (!education.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this education");
        }
        education.setInstitution(dto.getInstitution().trim());
        education.setDegree(dto.getDegree() != null ? dto.getDegree().trim() : null);
        education.setFieldOfStudy(dto.getFieldOfStudy() != null ? dto.getFieldOfStudy().trim() : null);
        education.setStartDate(dto.getStartDate());
        education.setEndDate(dto.getEndDate());
        Education saved = educationRepository.save(education);
        return new EducationDto(
                saved.getId(),
                saved.getInstitution(),
                saved.getDegree(),
                saved.getFieldOfStudy(),
                saved.getStartDate(),
                saved.getEndDate()
        );
    }

    public void deleteEducation(User user, Long educationId) {
        Education education = educationRepository.findById(educationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Education not found"));
        if (!education.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this education");
        }
        educationRepository.delete(education);
    }

    // ─── Experience ─────────────────────────────────────────────────────────

    public List<ExperienceDto> getExperiences(Long userId) {
        User user = getUser(userId);
        return experienceRepository.findByUserOrderByStartDateDesc(user)
                .stream()
                .map(exp -> new ExperienceDto(
                        exp.getId(),
                        exp.getCompanyName(),
                        exp.getJobTitle(),
                        exp.getStartDate(),
                        exp.getEndDate(),
                        exp.getDescription()
                ))
                .toList();
    }

    public ExperienceDto addExperience(User user, ExperienceDto dto) {
        Experience experience = new Experience(
                user,
                dto.getCompanyName().trim(),
                dto.getJobTitle().trim(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getDescription() != null ? dto.getDescription().replaceAll("<[^>]*>", "").trim() : null
        );
        Experience saved = experienceRepository.save(experience);
        return new ExperienceDto(
                saved.getId(),
                saved.getCompanyName(),
                saved.getJobTitle(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getDescription().replaceAll("<[^>]*>", "")
        );
    }

    public ExperienceDto updateExperience(User user, Long experienceId, ExperienceDto dto) {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experience not found"));
        if (!experience.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this experience");
        }
        experience.setCompanyName(dto.getCompanyName().trim());
        experience.setJobTitle(dto.getJobTitle().trim());
        experience.setStartDate(dto.getStartDate());
        experience.setEndDate(dto.getEndDate());
        experience.setDescription(dto.getDescription() != null ? dto.getDescription().replaceAll("<[^>]*>", "").trim() : null);
        Experience saved = experienceRepository.save(experience);
        return new ExperienceDto(
                saved.getId(),
                saved.getCompanyName(),
                saved.getJobTitle(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getDescription().replaceAll("<[^>]*>", "")
        );
    }

    public void deleteExperience(User user, Long experienceId) {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experience not found"));
        if (!experience.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this experience");
        }
        experienceRepository.delete(experience);
    }

    // ─── Project ────────────────────────────────────────────────────────────

    public List<ProjectDto> getProjects(Long userId) {
        User user = getUser(userId);
        return projectRepository.findByUserOrderByIdDesc(user)
                .stream()
                .map(p -> new ProjectDto(
                        p.getId(),
                        p.getProjectName(),
                        p.getDescription().replaceAll("<[^>]*>", ""),
                        p.getProjectUrl()
                ))
                .toList();
    }

    public ProjectDto addProject(User user, ProjectDto dto) {
        Project project = new Project(
                user,
                dto.getProjectName().trim(),
                dto.getDescription() != null ? dto.getDescription().replaceAll("<[^>]*>", "").trim() : null,
                dto.getProjectUrl() != null ? dto.getProjectUrl().trim() : null
        );
        Project saved = projectRepository.save(project);
        return new ProjectDto(
                saved.getId(),
                saved.getProjectName(),
                saved.getDescription().replaceAll("<[^>]*>", ""),
                saved.getProjectUrl()
        );
    }

    public ProjectDto updateProject(User user, Long projectId, ProjectDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        if (!project.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update this project");
        }
        project.setProjectName(dto.getProjectName().trim());
        project.setDescription(dto.getDescription() != null ? dto.getDescription().replaceAll("<[^>]*>", "").trim() : null);
        project.setProjectUrl(dto.getProjectUrl() != null ? dto.getProjectUrl().trim() : null);
        Project saved = projectRepository.save(project);
        return new ProjectDto(
                saved.getId(),
                saved.getProjectName(),
                saved.getDescription().replaceAll("<[^>]*>", ""),
                saved.getProjectUrl()
        );
    }

    public void deleteProject(User user, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        if (!project.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this project");
        }
        projectRepository.delete(project);
    }
}
