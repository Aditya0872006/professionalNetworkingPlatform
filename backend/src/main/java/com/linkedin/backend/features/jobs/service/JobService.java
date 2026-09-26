package com.linkedin.backend.features.jobs.service;

import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.authentication.model.UserStatus;
import com.linkedin.backend.features.jobs.dto.JobApplicationResponseDto;
import com.linkedin.backend.features.jobs.dto.JobDto;
import com.linkedin.backend.features.jobs.dto.JobResponseDto;
import com.linkedin.backend.features.jobs.model.ApplicationStatus;
import com.linkedin.backend.features.jobs.model.Job;
import com.linkedin.backend.features.jobs.model.JobApplication;
import com.linkedin.backend.features.jobs.model.JobStatus;
import com.linkedin.backend.features.jobs.repository.JobApplicationRepository;
import com.linkedin.backend.features.jobs.repository.JobRepository;
import com.linkedin.backend.features.notifications.service.NotificationService;
import com.linkedin.backend.features.profile.model.UserSkill;
import com.linkedin.backend.features.profile.repository.UserSkillRepository;
import com.linkedin.backend.features.recruiter.service.RecruiterService;
import com.linkedin.backend.features.storage.service.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final RecruiterService recruiterService;
    private final NotificationService notificationService;
    private final UserSkillRepository userSkillRepository;
    private final StorageService storageService;

    public JobService(JobRepository jobRepository,
                      JobApplicationRepository jobApplicationRepository,
                      RecruiterService recruiterService,
                      NotificationService notificationService,
                      UserSkillRepository userSkillRepository,
                      StorageService storageService) {
        this.jobRepository = jobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.recruiterService = recruiterService;
        this.notificationService = notificationService;
        this.userSkillRepository = userSkillRepository;
        this.storageService = storageService;
    }

    @Transactional
    public Job createJob(JobDto dto, User recruiter) {
        if (recruiter.getRole() != Role.ROLE_RECRUITER) {
            throw new IllegalArgumentException("Only recruiters can create jobs.");
        }
        if (!recruiterService.isApprovedRecruiter(recruiter.getId())) {
            throw new IllegalStateException("Only APPROVED recruiters can post jobs. Your recruiter approval is pending or rejected.");
        }

        Job job = new Job();
        job.setRecruiter(recruiter);
        job.setTitle(dto.title());
        job.setCompany(dto.company() != null && !dto.company().isBlank() ? dto.company() : recruiter.getCompany());
        job.setDescription(dto.description().replaceAll("<[^>]*>", ""));
        job.setLocation(dto.location());
        job.setEmploymentType(dto.employmentType());
        job.setWorkMode(dto.workMode());
        job.setSalary(dto.salary());
        job.setMinExperience(dto.minExperience());
        job.setMaxExperience(dto.maxExperience());
        job.setRequiredSkills(dto.requiredSkills());
        job.setEducationRequirement(dto.educationRequirement());
        job.setDeadline(dto.deadline());
        job.setNumberOfOpenings(dto.numberOfOpenings() != null ? dto.numberOfOpenings() : 1);
        job.setStatus(JobStatus.PUBLISHED); // Create directly as published

        Job savedJob = jobRepository.save(job);
        sendJobRecommendations(savedJob);
        return savedJob;
    }

    @Transactional
    public Job updateJob(Long jobId, JobDto dto, User recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        if (!job.getRecruiter().getId().equals(recruiter.getId()) && recruiter.getRole() != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Access denied: You do not own this job.");
        }

        job.setTitle(dto.title());
        job.setCompany(dto.company());
        job.setDescription(dto.description().replaceAll("<[^>]*>", ""));
        job.setLocation(dto.location());
        job.setEmploymentType(dto.employmentType());
        job.setWorkMode(dto.workMode());
        job.setSalary(dto.salary());
        job.setMinExperience(dto.minExperience());
        job.setMaxExperience(dto.maxExperience());
        job.setRequiredSkills(dto.requiredSkills());
        job.setEducationRequirement(dto.educationRequirement());
        job.setDeadline(dto.deadline());
        if (dto.numberOfOpenings() != null) {
            job.setNumberOfOpenings(dto.numberOfOpenings());
        }

        return jobRepository.save(job);
    }

    @Transactional
    public Job publishJob(Long jobId, User recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new IllegalArgumentException("Access denied: You do not own this job.");
        }

        if (!recruiterService.isApprovedRecruiter(recruiter.getId())) {
            throw new IllegalStateException("Only APPROVED recruiters can publish jobs.");
        }

        job.setStatus(JobStatus.PUBLISHED);
        Job saved = jobRepository.save(job);
        sendJobRecommendations(saved);
        return saved;
    }

    @Transactional
    public Job closeJob(Long jobId, User recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        if (!job.getRecruiter().getId().equals(recruiter.getId()) && recruiter.getRole() != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Access denied: You do not own this job.");
        }

        job.setStatus(JobStatus.CLOSED);
        return jobRepository.save(job);
    }

    public List<JobResponseDto> getMyJobs(User recruiter) {
        List<Job> jobs = jobRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiter.getId());
        return jobs.stream().map(job -> toResponseDto(job, recruiter.getId())).collect(Collectors.toList());
    }

    public List<JobResponseDto> getPublishedJobs(String query, String location, User currentUser) {
        String q = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String loc = (location != null && !location.trim().isEmpty()) ? location.trim() : null;

        List<Job> jobs = jobRepository.searchPublishedJobs(q, loc);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return jobs.stream().map(job -> toResponseDto(job, currentUserId)).collect(Collectors.toList());
    }

    public JobResponseDto getJobDetails(Long jobId, User currentUser) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return toResponseDto(job, currentUserId);
    }

    @Transactional
    public JobApplication applyForJob(Long jobId, MultipartFile resumeFile, User applicant) throws IOException {
        if (applicant.getRole() != Role.ROLE_USER) {
            throw new IllegalArgumentException("Only normal users can apply for jobs.");
        }

        if (applicant.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalArgumentException("Your account is blocked. You cannot apply for jobs.");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new IllegalArgumentException("This job is not currently open for applications.");
        }

        // Server-Side Application Deadline Validation
        if (job.getDeadline() != null && LocalDateTime.now().isAfter(job.getDeadline())) {
            throw new IllegalArgumentException("Application deadline has passed. New applications are no longer accepted.");
        }

        // Prevent Duplicate Application
        if (jobApplicationRepository.existsByJobIdAndApplicantId(jobId, applicant.getId())) {
            throw new IllegalArgumentException("You have already applied for this job.");
        }

        if (resumeFile == null || resumeFile.isEmpty()) {
            throw new IllegalArgumentException("Please upload your resume to apply.");
        }

        String resumeUrl = storageService.saveDocument(resumeFile);

        JobApplication application = new JobApplication(job, applicant, resumeUrl);
        return jobApplicationRepository.save(application);
    }

    public List<JobApplicationResponseDto> getMyApplications(User applicant) {
        List<JobApplication> apps = jobApplicationRepository.findByApplicantIdOrderByAppliedAtDesc(applicant.getId());
        return apps.stream().map(this::toApplicationResponseDto).collect(Collectors.toList());
    }

    public List<JobApplicationResponseDto> getApplicantsForJob(Long jobId, User recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        // Ownership enforcement: Recruiter A cannot access Recruiter B's applicants
        if (!job.getRecruiter().getId().equals(recruiter.getId()) && recruiter.getRole() != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Access denied: You do not own this job.");
        }

        List<JobApplication> apps = jobApplicationRepository.findByJobIdOrderByAppliedAtDesc(jobId);
        return apps.stream().map(this::toApplicationResponseDto).collect(Collectors.toList());
    }

    @Transactional
    public JobApplication updateApplicationStatus(Long applicationId, ApplicationStatus newStatus, User recruiter) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        Job job = application.getJob();
        if (!job.getRecruiter().getId().equals(recruiter.getId()) && recruiter.getRole() != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Access denied: You do not own this job.");
        }

        application.setStatus(newStatus);
        JobApplication saved = jobApplicationRepository.save(application);

        notificationService.sendApplicationStatusNotification(
                application.getApplicant(),
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                newStatus.name()
        );

        return saved;
    }

    public JobApplication getApplicationById(Long applicationId, User user) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        boolean isApplicant = application.getApplicant().getId().equals(user.getId());
        boolean isRecruiter = application.getJob().getRecruiter().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isApplicant && !isRecruiter && !isAdmin) {
            throw new IllegalArgumentException("Access denied: You are not authorized to view this application.");
        }

        return application;
    }

    @Transactional
    public Job removeJobByAdmin(Long jobId, String reason, User admin) {
        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Only administrators can remove jobs.");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found."));

        job.setStatus(JobStatus.REMOVED);
        job.setRemovalReason(reason != null ? reason : "Removed by administrator for policy violation.");
        job.setRemovedBy(admin.getEmail());
        job.setRemovedAt(LocalDateTime.now());

        Job saved = jobRepository.save(job);

        notificationService.sendJobRemovedByAdminNotification(job.getRecruiter(), job.getId(), job.getTitle());
        return saved;
    }

    public List<Job> getAllJobsForAdmin() {
        return jobRepository.findAllByOrderByCreatedAtDesc();
    }

    private void sendJobRecommendations(Job job) {
        if (job.getRequiredSkills() == null || job.getRequiredSkills().isEmpty()) {
            return;
        }

        try {
            List<UserSkill> matchedSkills = userSkillRepository.findBySkillNameIgnoreCaseIn(job.getRequiredSkills());
            Set<Long> notifiedUserIds = new HashSet<>();

            for (UserSkill userSkill : matchedSkills) {
                User candidate = userSkill.getUser();
                if (candidate != null
                        && !candidate.getId().equals(job.getRecruiter().getId())
                        && candidate.getStatus() == UserStatus.ACTIVE
                        && notifiedUserIds.add(candidate.getId())) {
                    notificationService.sendJobRecommendationNotification(candidate, job.getId(), job.getTitle(), job.getCompany());
                }
            }
        } catch (Exception e) {
            // Ignore recommendation failures to avoid interrupting job creation
        }
    }

    public JobResponseDto toResponseDto(Job job, Long currentUserId) {
        long count = jobApplicationRepository.countByJobId(job.getId());
        boolean hasApplied = currentUserId != null &&
                jobApplicationRepository.existsByJobIdAndApplicantId(job.getId(), currentUserId);

        String recruiterName = (job.getRecruiter() != null)
                ? (job.getRecruiter().getFirstName() + " " + job.getRecruiter().getLastName())
                : "Recruiter";

        return new JobResponseDto(
                job.getId(),
                job.getRecruiter() != null ? job.getRecruiter().getId() : null,
                recruiterName,
                job.getTitle(),
                job.getCompany(),
                job.getDescription(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getWorkMode(),
                job.getSalary(),
                job.getMinExperience(),
                job.getMaxExperience(),
                job.getRequiredSkills(),
                job.getEducationRequirement(),
                job.getDeadline(),
                job.getNumberOfOpenings(),
                job.getStatus(),
                count,
                job.isExpired(),
                hasApplied,
                job.getCreatedAt()
        );
    }

    private JobApplicationResponseDto toApplicationResponseDto(JobApplication app) {
        User applicant = app.getApplicant();
        List<String> skills = Collections.emptyList();
        if (applicant.getSkills() != null) {
            skills = applicant.getSkills().stream().map(UserSkill::getSkillName).collect(Collectors.toList());
        }

        return new JobApplicationResponseDto(
                app.getId(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getJob().getCompany(),
                applicant.getId(),
                applicant.getFirstName() + " " + applicant.getLastName(),
                applicant.getEmail(),
                applicant.getPosition(),
                applicant.getLocation(),
                applicant.getProfilePicture(),
                skills,
                app.getResumeUrl(),
                app.getStatus(),
                app.getAppliedAt()
        );
    }
}
