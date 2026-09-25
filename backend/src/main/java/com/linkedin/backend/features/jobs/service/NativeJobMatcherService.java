package com.linkedin.backend.features.jobs.service;

import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.jobs.model.Job;
import com.linkedin.backend.features.jobs.model.JobApplication;
import com.linkedin.backend.features.jobs.repository.JobApplicationRepository;
import com.linkedin.backend.features.jobs.repository.JobRepository;
import com.linkedin.backend.features.profile.model.Education;
import com.linkedin.backend.features.profile.model.Experience;
import com.linkedin.backend.features.profile.model.UserSkill;
import com.linkedin.backend.features.profile.repository.EducationRepository;
import com.linkedin.backend.features.profile.repository.ExperienceRepository;
import com.linkedin.backend.features.profile.repository.UserSkillRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NativeJobMatcherService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserSkillRepository userSkillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;

    public NativeJobMatcherService(JobRepository jobRepository,
                                   JobApplicationRepository jobApplicationRepository,
                                   UserSkillRepository userSkillRepository,
                                   ExperienceRepository experienceRepository,
                                   EducationRepository educationRepository) {
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userSkillRepository = userSkillRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
    }

    public record CandidateScoreResult(
            double score, // 0.0 to 1.0
            int matchPercentage, // 0 to 100
            List<String> matchedSkills,
            List<String> missingSkills,
            double totalYearsExperience
    ) {}

    /**
     * Calculates the composite multi-dimensional match score between a candidate and a job.
     */
    public CandidateScoreResult calculateCandidateScore(Job job, User candidate) {
        // 1. SKILLS MATCH (50% Weight)
        List<String> requiredSkills = job.getRequiredSkills() != null ? job.getRequiredSkills() : Collections.emptyList();
        List<UserSkill> userSkills = userSkillRepository.findByUserIdOrderByIdAsc(candidate.getId());

        Set<String> candidateSkillSet = userSkills.stream()
                .map(s -> normalize(s.getSkillName()))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String reqSkill : requiredSkills) {
            String normalizedReq = normalize(reqSkill);
            if (candidateSkillSet.contains(normalizedReq) || containsSkillFuzzy(candidateSkillSet, normalizedReq)) {
                matchedSkills.add(reqSkill);
            } else {
                missingSkills.add(reqSkill);
            }
        }

        double skillScore;
        if (requiredSkills.isEmpty()) {
            skillScore = 0.85; // neutral-high if no specific skills specified
        } else {
            skillScore = (double) matchedSkills.size() / requiredSkills.size();
        }

        // 2. EXPERIENCE MATCH (25% Weight)
        List<Experience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(candidate.getId());
        double totalYears = 0.0;
        for (Experience exp : experiences) {
            if (exp.getStartDate() != null) {
                LocalDate end = exp.getEndDate() != null ? exp.getEndDate() : LocalDate.now();
                long days = ChronoUnit.DAYS.between(exp.getStartDate(), end);
                if (days > 0) {
                    totalYears += days / 365.25;
                }
            } else {
                totalYears += 1.0;
            }
        }

        double experienceScore = 1.0;
        Integer minExp = job.getMinExperience();
        if (minExp != null && minExp > 0) {
            if (totalYears >= minExp) {
                experienceScore = 1.0;
            } else if (totalYears > 0) {
                experienceScore = Math.max(0.25, totalYears / minExp);
            } else {
                experienceScore = 0.20;
            }
        }

        // 3. ROLE & KEYWORD MATCH (15% Weight)
        double roleScore = evaluateRoleMatch(job, candidate, experiences);

        // 4. EDUCATION MATCH (10% Weight)
        double educationScore = evaluateEducationMatch(job, candidate);

        // Weighted Composite Score: 50% Skills + 25% Experience + 15% Role + 10% Education
        double composite = (0.50 * skillScore) + (0.25 * experienceScore) + (0.15 * roleScore) + (0.10 * educationScore);

        // Safety clamp: if user matched 80%+ of skills, score shouldn't be penalized below 0.80
        if (!requiredSkills.isEmpty() && (double) matchedSkills.size() / requiredSkills.size() >= 0.80) {
            composite = Math.max(composite, 0.80);
        }

        double finalScore = Math.round(composite * 100.0) / 100.0;
        int matchPercentage = (int) Math.round(finalScore * 100);

        return new CandidateScoreResult(
                finalScore,
                matchPercentage,
                matchedSkills,
                missingSkills,
                Math.round(totalYears * 10.0) / 10.0
        );
    }

    /**
     * Returns the Top 3 matching applicants for a job for the recruiter view.
     */
    public Map<String, Object> getTopMatchingApplicants(Long jobId, User recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        if (!job.getRecruiter().getId().equals(recruiter.getId()) && recruiter.getRole() != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Access denied: You do not own this job.");
        }

        List<JobApplication> applications = jobApplicationRepository.findByJobIdOrderByAppliedAtDesc(jobId);

        record ScoredApp(JobApplication app, double score) {}

        List<ScoredApp> scoredApps = new ArrayList<>();
        for (JobApplication app : applications) {
            CandidateScoreResult res = calculateCandidateScore(job, app.getApplicant());
            scoredApps.add(new ScoredApp(app, res.score()));
        }

        // Sort descending by score
        scoredApps.sort((a, b) -> Double.compare(b.score(), a.score()));

        // Take top 3
        int limit = Math.min(3, scoredApps.size());
        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            ScoredApp sa = scoredApps.get(i);
            Map<String, Object> map = new HashMap<>();
            map.put("filename", sa.app().getResumeUrl());
            map.put("similarity_score", sa.score());
            results.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Top 3 matching resumes");
        response.put("results", results);
        return response;
    }

    private double evaluateRoleMatch(Job job, User candidate, List<Experience> experiences) {
        String jobTitle = normalize(job.getTitle());
        String userPosition = normalize(candidate.getPosition());

        if (!jobTitle.isEmpty() && !userPosition.isEmpty()) {
            if (userPosition.contains(jobTitle) || jobTitle.contains(userPosition)) {
                return 1.0;
            }
            Set<String> jobTokens = Arrays.stream(jobTitle.split("\\s+")).collect(Collectors.toSet());
            Set<String> userTokens = Arrays.stream(userPosition.split("\\s+")).collect(Collectors.toSet());
            long overlap = jobTokens.stream().filter(userTokens::contains).count();
            if (overlap > 0) {
                return 0.85;
            }
        }

        for (Experience exp : experiences) {
            String expTitle = normalize(exp.getJobTitle());
            if (!expTitle.isEmpty() && (expTitle.contains(jobTitle) || jobTitle.contains(expTitle))) {
                return 0.80;
            }
        }

        return candidate.getPosition() != null && !candidate.getPosition().isBlank() ? 0.60 : 0.40;
    }

    private double evaluateEducationMatch(Job job, User candidate) {
        String reqEdu = job.getEducationRequirement();
        if (reqEdu == null || reqEdu.isBlank()) {
            return 1.0;
        }

        String normReq = normalize(reqEdu);
        List<Education> educations = educationRepository.findByUserIdOrderByStartDateDesc(candidate.getId());
        if (educations.isEmpty()) {
            return 0.50;
        }

        for (Education edu : educations) {
            String degree = normalize(edu.getDegree());
            String field = normalize(edu.getFieldOfStudy());
            if (degree.contains(normReq) || normReq.contains(degree) ||
                    field.contains(normReq) || normReq.contains(field)) {
                return 1.0;
            }
        }
        return 0.70;
    }

    private boolean containsSkillFuzzy(Set<String> candidateSkills, String target) {
        for (String skill : candidateSkills) {
            if (skill.contains(target) || target.contains(skill)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("[^a-z0-9#+.]", " ").replaceAll("\\s+", " ").trim();
    }
}
