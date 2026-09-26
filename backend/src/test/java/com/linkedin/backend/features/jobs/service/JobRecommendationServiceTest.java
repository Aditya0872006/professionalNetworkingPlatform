package com.linkedin.backend.features.jobs.service;

import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.jobs.dto.JobRecommendationDto;
import com.linkedin.backend.features.jobs.dto.JobResponseDto;
import com.linkedin.backend.features.jobs.model.EmploymentType;
import com.linkedin.backend.features.jobs.model.Job;
import com.linkedin.backend.features.jobs.model.JobStatus;
import com.linkedin.backend.features.jobs.model.WorkMode;
import com.linkedin.backend.features.jobs.repository.JobApplicationRepository;
import com.linkedin.backend.features.jobs.repository.JobRepository;
import com.linkedin.backend.features.profile.model.Education;
import com.linkedin.backend.features.profile.model.Experience;
import com.linkedin.backend.features.profile.model.UserSkill;
import com.linkedin.backend.features.profile.repository.EducationRepository;
import com.linkedin.backend.features.profile.repository.ExperienceRepository;
import com.linkedin.backend.features.profile.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRecommendationServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private UserSkillRepository userSkillRepository;

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private EducationRepository educationRepository;

    @Mock
    private JobService jobService;

    @InjectMocks
    private JobRecommendationService jobRecommendationService;

    private User candidate;
    private Job matchingJob;
    private Job unmatchingJob;

    @BeforeEach
    void setUp() {
        candidate = new User();
        candidate.setId(100L);
        candidate.setRole(Role.ROLE_USER);
        candidate.setFirstName("Alice");
        candidate.setLastName("Dev");
        candidate.setPosition("Full Stack Software Engineer");
        candidate.setLocation("San Francisco, CA");
        candidate.setAbout("Passionate Full Stack engineer with expertise in React, TypeScript, and Java Spring Boot.");

        matchingJob = new Job();
        matchingJob.setId(1L);
        matchingJob.setTitle("Senior Full Stack Software Engineer");
        matchingJob.setCompany("Tech Corp");
        matchingJob.setDescription("Looking for a full stack engineer skilled in React and Spring Boot.");
        matchingJob.setLocation("San Francisco, CA");
        matchingJob.setEmploymentType(EmploymentType.FULL_TIME);
        matchingJob.setWorkMode(WorkMode.HYBRID);
        matchingJob.setMinExperience(2);
        matchingJob.setRequiredSkills(List.of("React", "Spring Boot", "TypeScript"));
        matchingJob.setEducationRequirement("Computer Science");
        matchingJob.setStatus(JobStatus.PUBLISHED);

        unmatchingJob = new Job();
        unmatchingJob.setId(2L);
        unmatchingJob.setTitle("Marketing Specialist");
        unmatchingJob.setCompany("Ads Inc");
        unmatchingJob.setDescription("Digital marketing and SEO strategy.");
        unmatchingJob.setLocation("New York, NY");
        unmatchingJob.setEmploymentType(EmploymentType.FULL_TIME);
        unmatchingJob.setWorkMode(WorkMode.ON_SITE);
        unmatchingJob.setMinExperience(5);
        unmatchingJob.setRequiredSkills(List.of("SEO", "Content Marketing", "Copywriting"));
        unmatchingJob.setEducationRequirement("Marketing or Communications");
        unmatchingJob.setStatus(JobStatus.PUBLISHED);
    }

    @Test
    void testEvaluateJobForUser_HighMatch() {
        List<UserSkill> skills = List.of(
                new UserSkill(candidate, "React"),
                new UserSkill(candidate, "Spring Boot"),
                new UserSkill(candidate, "TypeScript")
        );

        Experience exp = new Experience(
                candidate, "Tech Solutions", "Full Stack Developer",
                LocalDate.now().minusYears(3), LocalDate.now(), "Built web services"
        );
        List<Experience> experiences = List.of(exp);

        Education edu = new Education(
                candidate, "University", "Bachelor of Science", "Computer Science",
                LocalDate.of(2018, 9, 1), LocalDate.of(2022, 5, 1)
        );
        List<Education> educations = List.of(edu);

        JobResponseDto mockResponseDto = new JobResponseDto(
                matchingJob.getId(), 50L, "Recruiter Bob", matchingJob.getTitle(),
                matchingJob.getCompany(), matchingJob.getDescription(), matchingJob.getLocation(),
                matchingJob.getEmploymentType(), matchingJob.getWorkMode(), "$140,000",
                matchingJob.getMinExperience(), 5, matchingJob.getRequiredSkills(),
                matchingJob.getEducationRequirement(), LocalDateTime.now().plusDays(30), 2,
                JobStatus.PUBLISHED, 5L, false, false, LocalDateTime.now()
        );

        when(jobService.toResponseDto(eq(matchingJob), eq(100L))).thenReturn(mockResponseDto);

        JobRecommendationDto result = jobRecommendationService.evaluateJobForUser(
                matchingJob, candidate, skills, experiences, educations, 3.0, false
        );

        assertNotNull(result);
        assertTrue(result.matchPercentage() >= 80, "Expected match percentage to be >= 80% for high match");
        assertEquals("TOP_MATCH", result.recommendationTier());
        assertTrue(result.matchedSkills().contains("React"));
        assertTrue(result.matchedSkills().contains("Spring Boot"));
        assertTrue(result.matchedSkills().contains("TypeScript"));
        assertTrue(result.experienceMatched());
        assertTrue(result.educationMatched());
        assertTrue(result.locationMatched());
        assertFalse(result.aiReasoning().isBlank());
        assertFalse(result.matchHighlights().isEmpty());
    }

    @Test
    void testGetRecommendationsForUser_SortedByScore() {
        List<UserSkill> skills = List.of(
                new UserSkill(candidate, "React"),
                new UserSkill(candidate, "Spring Boot")
        );
        List<Experience> experiences = List.of(
                new Experience(candidate, "Startup", "Software Engineer", LocalDate.now().minusYears(2), null, "Coding")
        );
        List<Education> educations = List.of(
                new Education(candidate, "College", "B.Tech", "Computer Science", null, null)
        );

        when(userSkillRepository.findByUserIdOrderByIdAsc(100L)).thenReturn(skills);
        when(experienceRepository.findByUserIdOrderByStartDateDesc(100L)).thenReturn(experiences);
        when(educationRepository.findByUserIdOrderByStartDateDesc(100L)).thenReturn(educations);
        when(jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED)).thenReturn(List.of(unmatchingJob, matchingJob));
        when(jobApplicationRepository.existsByJobIdAndApplicantId(any(), eq(100L))).thenReturn(false);

        JobResponseDto dtoMatching = new JobResponseDto(
                matchingJob.getId(), 50L, "Recruiter Bob", matchingJob.getTitle(),
                matchingJob.getCompany(), matchingJob.getDescription(), matchingJob.getLocation(),
                matchingJob.getEmploymentType(), matchingJob.getWorkMode(), "$140,000",
                matchingJob.getMinExperience(), 5, matchingJob.getRequiredSkills(),
                matchingJob.getEducationRequirement(), null, 2,
                JobStatus.PUBLISHED, 0L, false, false, LocalDateTime.now()
        );
        JobResponseDto dtoUnmatching = new JobResponseDto(
                unmatchingJob.getId(), 51L, "Recruiter Charlie", unmatchingJob.getTitle(),
                unmatchingJob.getCompany(), unmatchingJob.getDescription(), unmatchingJob.getLocation(),
                unmatchingJob.getEmploymentType(), unmatchingJob.getWorkMode(), "$90,000",
                unmatchingJob.getMinExperience(), 5, unmatchingJob.getRequiredSkills(),
                unmatchingJob.getEducationRequirement(), null, 1,
                JobStatus.PUBLISHED, 0L, false, false, LocalDateTime.now()
        );

        when(jobService.toResponseDto(eq(matchingJob), eq(100L))).thenReturn(dtoMatching);
        when(jobService.toResponseDto(eq(unmatchingJob), eq(100L))).thenReturn(dtoUnmatching);

        List<JobRecommendationDto> recs = jobRecommendationService.getRecommendationsForUser(
                candidate, 0.0, 10, false
        );

        assertEquals(2, recs.size());
        assertEquals(matchingJob.getId(), recs.get(0).job().id(), "Highest matching job should be ranked first");
        assertTrue(recs.get(0).matchScore() > recs.get(1).matchScore());
    }
}
