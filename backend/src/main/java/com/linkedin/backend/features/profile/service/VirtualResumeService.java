package com.linkedin.backend.features.profile.service;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.profile.model.Education;
import com.linkedin.backend.features.profile.model.Experience;
import com.linkedin.backend.features.profile.model.Project;
import com.linkedin.backend.features.profile.model.UserSkill;
import com.linkedin.backend.features.profile.repository.EducationRepository;
import com.linkedin.backend.features.profile.repository.ExperienceRepository;
import com.linkedin.backend.features.profile.repository.ProjectRepository;
import com.linkedin.backend.features.profile.repository.UserSkillRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VirtualResumeService {

    private final UserSkillRepository userSkillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;

    public VirtualResumeService(UserSkillRepository userSkillRepository,
                                ExperienceRepository experienceRepository,
                                EducationRepository educationRepository,
                                ProjectRepository projectRepository) {
        this.userSkillRepository = userSkillRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.projectRepository = projectRepository;
    }

    public String generateVirtualResumeText(User user) {
        StringBuilder sb = new StringBuilder();

        // Candidate Header
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "")).trim();
        if (fullName.isEmpty()) {
            fullName = "Candidate";
        }
        sb.append(fullName).append("\n");

        if (user.getPosition() != null && !user.getPosition().isBlank()) {
            sb.append("Professional Title: ").append(user.getPosition());
            if (user.getCompany() != null && !user.getCompany().isBlank()) {
                sb.append(" at ").append(user.getCompany());
            }
            sb.append("\n");
        }

        if (user.getLocation() != null && !user.getLocation().isBlank()) {
            sb.append("Location: ").append(user.getLocation()).append("\n");
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            sb.append("Email: ").append(user.getEmail()).append("\n");
        }
        sb.append("\n");

        // Professional Summary / About
        if (user.getAbout() != null && !user.getAbout().isBlank()) {
            sb.append("PROFESSIONAL SUMMARY\n");
            sb.append(user.getAbout().trim()).append("\n\n");
        }

        // Skills
        List<UserSkill> skills = userSkillRepository.findByUserIdOrderByIdAsc(user.getId());
        if (!skills.isEmpty()) {
            sb.append("TECHNICAL & PROFESSIONAL SKILLS\n");
            String skillList = skills.stream()
                    .map(UserSkill::getSkillName)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.joining(", "));
            sb.append(skillList).append("\n\n");
        }

        // Experience
        List<Experience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(user.getId());
        if (!experiences.isEmpty()) {
            sb.append("WORK EXPERIENCE\n");
            for (Experience exp : experiences) {
                sb.append("• ").append(exp.getJobTitle() != null ? exp.getJobTitle() : "Role")
                        .append(" at ").append(exp.getCompanyName() != null ? exp.getCompanyName() : "Company");
                if (exp.getStartDate() != null) {
                    sb.append(" (").append(exp.getStartDate())
                            .append(" - ").append(exp.getEndDate() != null ? exp.getEndDate() : "Present").append(")");
                }
                sb.append("\n");
                if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                    sb.append("  ").append(exp.getDescription().trim()).append("\n");
                }
            }
            sb.append("\n");
        }

        // Education
        List<Education> educations = educationRepository.findByUserIdOrderByStartDateDesc(user.getId());
        if (!educations.isEmpty()) {
            sb.append("EDUCATION\n");
            for (Education edu : educations) {
                sb.append("• ").append(edu.getDegree() != null ? edu.getDegree() : "Degree");
                if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().isBlank()) {
                    sb.append(" in ").append(edu.getFieldOfStudy());
                }
                sb.append(", ").append(edu.getInstitution());
                if (edu.getStartDate() != null) {
                    sb.append(" (").append(edu.getStartDate())
                            .append(" - ").append(edu.getEndDate() != null ? edu.getEndDate() : "Present").append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // Projects
        List<Project> projects = projectRepository.findByUserIdOrderByIdDesc(user.getId());
        if (!projects.isEmpty()) {
            sb.append("PROJECTS\n");
            for (Project proj : projects) {
                sb.append("• ").append(proj.getProjectName()).append("\n");
                if (proj.getDescription() != null && !proj.getDescription().isBlank()) {
                    sb.append("  ").append(proj.getDescription().trim()).append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
