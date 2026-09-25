package com.linkedin.backend.features.jobs.service;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.jobs.dto.TopApplicantEvaluationDto;
import com.linkedin.backend.features.jobs.model.Job;
import com.linkedin.backend.features.jobs.model.JobApplication;
import com.linkedin.backend.features.jobs.repository.JobApplicationRepository;
import com.linkedin.backend.features.jobs.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JobFitService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final NativeJobMatcherService nativeJobMatcherService;

    public JobFitService(JobRepository jobRepository,
                         JobApplicationRepository jobApplicationRepository,
                         NativeJobMatcherService nativeJobMatcherService) {
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.nativeJobMatcherService = nativeJobMatcherService;
    }

    public TopApplicantEvaluationDto evaluateCandidateFit(Long jobId, User candidate) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        // 1. Calculate native score for the candidate
        NativeJobMatcherService.CandidateScoreResult candidateResult =
                nativeJobMatcherService.calculateCandidateScore(job, candidate);

        double candidateScore = candidateResult.score();
        int matchPercentage = candidateResult.matchPercentage();

        // 2. Calculate scores for all existing applicants for competitive ranking
        List<JobApplication> existingApplications = jobApplicationRepository.findByJobIdOrderByAppliedAtDesc(jobId);
        int totalCompetitors = existingApplications.size();
        boolean isEarlyApplicant = totalCompetitors < 5;

        List<Double> otherApplicantScores = new ArrayList<>();
        for (JobApplication app : existingApplications) {
            // Avoid comparing candidate against their own previous application if already applied
            if (!app.getApplicant().getId().equals(candidate.getId())) {
                NativeJobMatcherService.CandidateScoreResult otherRes =
                        nativeJobMatcherService.calculateCandidateScore(job, app.getApplicant());
                otherApplicantScores.add(otherRes.score());
            }
        }

        // 3. Compute Rank and Percentile
        int rank = 1;
        for (Double otherScore : otherApplicantScores) {
            if (otherScore > candidateScore) {
                rank++;
            }
        }

        Integer percentile = null;
        if (!otherApplicantScores.isEmpty()) {
            percentile = Math.max(1, (int) Math.round(((double) rank / (otherApplicantScores.size() + 1)) * 100));
        }

        // 4. Determine Top Applicant status
        boolean isTopApplicant;
        if (otherApplicantScores.isEmpty()) {
            isTopApplicant = candidateScore >= 0.60 ||
                    (!candidateResult.matchedSkills().isEmpty() && candidateResult.missingSkills().isEmpty());
        } else {
            isTopApplicant = rank <= 3 || (percentile != null && percentile <= 25) || candidateScore >= 0.70;
        }

        String matchTier;
        if (candidateScore >= 0.80 || (percentile != null && percentile <= 10)) {
            matchTier = "TOP_APPLICANT";
        } else if (candidateScore >= 0.60 || (percentile != null && percentile <= 30)) {
            matchTier = "STRONG_MATCH";
        } else if (candidateScore >= 0.40) {
            matchTier = "MODERATE_MATCH";
        } else {
            matchTier = "GROWTH_OPPORTUNITY";
        }

        String headlineMessage = generateHeadlineMessage(
                isTopApplicant,
                isEarlyApplicant,
                matchPercentage,
                rank,
                otherApplicantScores.size(),
                percentile
        );

        return new TopApplicantEvaluationDto(
                isTopApplicant,
                candidateScore,
                matchPercentage,
                otherApplicantScores.isEmpty() ? 1 : rank,
                otherApplicantScores.size(),
                percentile,
                isEarlyApplicant,
                candidateResult.matchedSkills(),
                candidateResult.missingSkills(),
                headlineMessage,
                matchTier
        );
    }

    private String generateHeadlineMessage(
            boolean isTopApplicant,
            boolean isEarlyApplicant,
            int matchPercentage,
            int rank,
            int totalCompetitors,
            Integer percentile) {

        if (isTopApplicant && totalCompetitors > 0 && percentile != null) {
            return String.format("You'd be a top applicant! Your profile ranks in the top %d%% of applicants (%d%% match).",
                    percentile, matchPercentage);
        } else if (isTopApplicant && totalCompetitors == 0) {
            return String.format("You'd be a top applicant! Your profile has a strong %d%% match for this role.",
                    matchPercentage);
        } else if (isEarlyApplicant && totalCompetitors == 0) {
            return String.format("Be the first applicant! Your profile has a %d%% match for this opening.",
                    matchPercentage);
        } else if (isEarlyApplicant) {
            return String.format("Be an early applicant! You match %d%% of the role with only %d other applicant(s).",
                    matchPercentage, totalCompetitors);
        } else {
            return String.format("Your profile matches %d%% of the requirements for this role.",
                    matchPercentage);
        }
    }
}
