package com.linkedin.backend.features.jobs.service;

import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.jobs.dto.JobRecommendationDto;
import com.linkedin.backend.features.jobs.dto.JobResponseDto;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobRecommendationService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserSkillRepository userSkillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final JobService jobService;

    public JobRecommendationService(JobRepository jobRepository,
                                  JobApplicationRepository jobApplicationRepository,
                                  UserSkillRepository userSkillRepository,
                                  ExperienceRepository experienceRepository,
                                  EducationRepository educationRepository,
                                  JobService jobService) {
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userSkillRepository = userSkillRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.jobService = jobService;
    }

    /**
     * Generates personalized AI job recommendations for the given candidate.
     */
    public List<JobRecommendationDto> getRecommendationsForUser(User user, double minScore, int limit, boolean includeApplied) {
        if (user.getRole() != Role.ROLE_USER) {
            return Collections.emptyList();
        }

        // Fetch candidate profile signals
        List<UserSkill> skills = userSkillRepository.findByUserIdOrderByIdAsc(user.getId());
        List<Experience> experiences = experienceRepository.findByUserIdOrderByStartDateDesc(user.getId());
        List<Education> educations = educationRepository.findByUserIdOrderByStartDateDesc(user.getId());

        // Calculate candidate's total years of experience
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
        double candidateExperienceYears = Math.round(totalYears * 10.0) / 10.0;

        // Fetch active published jobs
        List<Job> publishedJobs = jobRepository.findByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED);

        List<JobRecommendationDto> recommendations = new ArrayList<>();

        for (Job job : publishedJobs) {
            if (job.isExpired()) {
                continue;
            }

            boolean hasApplied = jobApplicationRepository.existsByJobIdAndApplicantId(job.getId(), user.getId());
            if (hasApplied && !includeApplied) {
                continue;
            }

            JobRecommendationDto recommendation = evaluateJobForUser(
                    job, user, skills, experiences, educations, candidateExperienceYears, hasApplied
            );

            if (recommendation.matchScore() >= minScore) {
                recommendations.add(recommendation);
            }
        }

        // Sort descending by match score
        recommendations.sort((a, b) -> Double.compare(b.matchScore(), a.matchScore()));

        if (limit > 0 && recommendations.size() > limit) {
            return recommendations.subList(0, limit);
        }

        return recommendations;
    }

    /**
     * Evaluates a single job against the candidate's profile across 4 dimensions.
     */
    public JobRecommendationDto evaluateJobForUser(Job job,
                                                 User user,
                                                 List<UserSkill> skills,
                                                 List<Experience> experiences,
                                                 List<Education> educations,
                                                 double candidateExperienceYears,
                                                 boolean hasApplied) {
        // 1. SKILLS EVALUATION (40% Weight)
        List<String> requiredSkills = job.getRequiredSkills() != null ? job.getRequiredSkills() : Collections.emptyList();
        Set<String> userSkillSet = skills.stream()
                .map(s -> normalize(s.getSkillName()))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String req : requiredSkills) {
            String normReq = normalize(req);
            if (userSkillSet.contains(normReq) || containsSkillFuzzy(userSkillSet, normReq)) {
                matchedSkills.add(req);
            } else {
                missingSkills.add(req);
            }
        }

        double skillScore;
        if (requiredSkills.isEmpty()) {
            // Check if any candidate skills appear in job description
            long mentions = userSkillSet.stream()
                    .filter(s -> !s.isEmpty() && normalize(job.getDescription()).contains(s))
                    .count();
            skillScore = mentions > 0 ? 0.85 : 0.65;
        } else {
            skillScore = (double) matchedSkills.size() / requiredSkills.size();
        }

        // 2. EXPERIENCE EVALUATION (25% Weight)
        double experienceScore = 1.0;
        Integer minExp = job.getMinExperience();
        boolean experienceMatched = true;

        if (minExp != null && minExp > 0) {
            if (candidateExperienceYears >= minExp) {
                Integer maxExp = job.getMaxExperience();
                if (maxExp != null && candidateExperienceYears > maxExp + 4) {
                    experienceScore = 0.90; // slightly senior
                } else {
                    experienceScore = 1.0;
                }
                experienceMatched = true;
            } else if (candidateExperienceYears > 0) {
                experienceScore = Math.max(0.25, candidateExperienceYears / minExp);
                experienceMatched = false;
            } else {
                experienceScore = 0.20;
                experienceMatched = false;
            }
        }

        // 3. PROFILE & ROLE EVALUATION (20% Weight)
        double roleScore = evaluateRoleMatch(job, user, experiences);
        boolean locationMatched = evaluateLocationMatch(job, user);
        double locationScore = locationMatched ? 1.0 : 0.65;
        double profileScore = (0.75 * roleScore) + (0.25 * locationScore);

        // 4. EDUCATION EVALUATION (15% Weight)
        boolean educationMatched = true;
        double educationScore = 1.0;
        String reqEdu = job.getEducationRequirement();

        if (reqEdu != null && !reqEdu.isBlank()) {
            String normReq = normalize(reqEdu);
            if (educations.isEmpty()) {
                educationScore = 0.45;
                educationMatched = false;
            } else {
                boolean foundDegreeMatch = false;
                for (Education edu : educations) {
                    String deg = normalize(edu.getDegree());
                    String field = normalize(edu.getFieldOfStudy());
                    if (deg.contains(normReq) || normReq.contains(deg) ||
                            field.contains(normReq) || normReq.contains(field)) {
                        foundDegreeMatch = true;
                        break;
                    }
                }
                if (foundDegreeMatch) {
                    educationScore = 1.0;
                    educationMatched = true;
                } else {
                    educationScore = 0.70; // Has degree, but field not exact match
                    educationMatched = false;
                }
            }
        }

        // COMPOSITE SCORE (40% Skills, 25% Experience, 20% Profile, 15% Education)
        double composite = (0.40 * skillScore) + (0.25 * experienceScore) + (0.20 * profileScore) + (0.15 * educationScore);

        // Safety clamp: if candidate matched >= 80% skills, maintain a minimum score of 0.75
        if (!requiredSkills.isEmpty() && (double) matchedSkills.size() / requiredSkills.size() >= 0.80) {
            composite = Math.max(composite, 0.75);
        }

        double finalScore = Math.min(1.0, Math.round(composite * 100.0) / 100.0);
        int matchPercentage = (int) Math.round(finalScore * 100);

        // Recommendation Tier
        String recommendationTier;
        if (matchPercentage >= 85) {
            recommendationTier = "TOP_MATCH";
        } else if (matchPercentage >= 70) {
            recommendationTier = "STRONG_MATCH";
        } else if (matchPercentage >= 50) {
            recommendationTier = "GOOD_MATCH";
        } else {
            recommendationTier = "EXPLORE";
        }

        // Build Highlights
        List<String> matchHighlights = new ArrayList<>();
        if (!matchedSkills.isEmpty()) {
            matchHighlights.add("Matches " + matchedSkills.size() + " of your verified skills: " + String.join(", ", matchedSkills));
        }
        if (minExp != null && minExp > 0) {
            if (candidateExperienceYears >= minExp) {
                matchHighlights.add("Your " + candidateExperienceYears + " yrs experience fulfills the " + minExp + "+ yrs requirement");
            } else {
                matchHighlights.add("Role requires " + minExp + " yrs experience (You have " + candidateExperienceYears + " yrs)");
            }
        } else {
            matchHighlights.add("Great fit for all experience levels (" + candidateExperienceYears + " yrs in profile)");
        }
        if (roleScore >= 0.80) {
            matchHighlights.add("High alignment with your current professional title and career focus");
        }
        if (locationMatched) {
            matchHighlights.add(job.getWorkMode() == WorkMode.REMOTE ? "Remote opportunity matching workplace flexibility" : "Direct location match in " + job.getLocation());
        }
        if (educationMatched && reqEdu != null && !reqEdu.isBlank()) {
            matchHighlights.add("Education background aligns with requirements (" + reqEdu + ")");
        }

        // Build AI Reasoning
        String aiReasoning = generateAiReasoning(
                job, matchPercentage, recommendationTier, matchedSkills, candidateExperienceYears, minExp, locationMatched
        );

        // Score Breakdown
        Map<String, Integer> scoreBreakdown = new LinkedHashMap<>();
        scoreBreakdown.put("skills", (int) Math.round(skillScore * 100));
        scoreBreakdown.put("experience", (int) Math.round(experienceScore * 100));
        scoreBreakdown.put("profile", (int) Math.round(profileScore * 100));
        scoreBreakdown.put("education", (int) Math.round(educationScore * 100));

        JobResponseDto jobDto = jobService.toResponseDto(job, user.getId());

        return new JobRecommendationDto(
                jobDto,
                finalScore,
                matchPercentage,
                recommendationTier,
                matchedSkills,
                missingSkills,
                candidateExperienceYears,
                minExp,
                experienceMatched,
                educationMatched,
                locationMatched,
                aiReasoning,
                matchHighlights,
                scoreBreakdown
        );
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

        // Check if candidate bio mentions job title keywords
        if (candidate.getAbout() != null && !candidate.getAbout().isBlank()) {
            String normAbout = normalize(candidate.getAbout());
            if (normAbout.contains(jobTitle)) {
                return 0.75;
            }
        }

        return candidate.getPosition() != null && !candidate.getPosition().isBlank() ? 0.60 : 0.40;
    }

    private boolean evaluateLocationMatch(Job job, User candidate) {
        if (job.getWorkMode() == WorkMode.REMOTE) {
            return true;
        }
        if (job.getLocation() == null || candidate.getLocation() == null) {
            return true;
        }
        String jobLoc = normalize(job.getLocation());
        String userLoc = normalize(candidate.getLocation());
        return jobLoc.isEmpty() || userLoc.isEmpty() || jobLoc.contains(userLoc) || userLoc.contains(jobLoc);
    }

    private String generateAiReasoning(Job job,
                                      int matchPercentage,
                                      String tier,
                                      List<String> matchedSkills,
                                      double userExp,
                                      Integer reqExp,
                                      boolean locationMatched) {
        StringBuilder sb = new StringBuilder();
        if ("TOP_MATCH".equals(tier)) {
            sb.append("Outstanding match! ");
        } else if ("STRONG_MATCH".equals(tier)) {
            sb.append("Strong fit for your background! ");
        } else if ("GOOD_MATCH".equals(tier)) {
            sb.append("Promising opportunity for your skillset. ");
        } else {
            sb.append("Growth opportunity to explore new skills. ");
        }

        if (!matchedSkills.isEmpty()) {
            sb.append("Your expertise in ").append(String.join(", ", matchedSkills.stream().limit(3).toList()));
            if (matchedSkills.size() > 3) {
                sb.append(" and ").append(matchedSkills.size() - 3).append(" other skills");
            }
            sb.append(" aligns directly with this role. ");
        }

        if (reqExp != null && reqExp > 0 && userExp >= reqExp) {
            sb.append("Your ").append(userExp).append(" years of experience comfortably fulfill the requirements. ");
        }

        if (job.getWorkMode() == WorkMode.REMOTE) {
            sb.append("Offers remote flexibility.");
        } else if (locationMatched && job.getLocation() != null && !job.getLocation().isBlank()) {
            sb.append("Conveniently located in ").append(job.getLocation()).append(".");
        }

        return sb.toString().trim();
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
