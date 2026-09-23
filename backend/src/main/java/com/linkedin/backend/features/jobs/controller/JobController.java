package com.linkedin.backend.features.jobs.controller;

import com.linkedin.backend.dto.Response;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.jobs.dto.JobDto;
import com.linkedin.backend.features.jobs.dto.JobApplicationResponseDto;
import com.linkedin.backend.features.jobs.dto.JobResponseDto;
import com.linkedin.backend.features.jobs.model.ApplicationStatus;
import com.linkedin.backend.features.jobs.model.Job;
import com.linkedin.backend.features.jobs.model.JobApplication;
import com.linkedin.backend.features.jobs.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<List<JobResponseDto>> getPublishedJobs(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "location", required = false) String location,
            @RequestAttribute("authenticatedUser") User user) {
        List<JobResponseDto> jobs = jobService.getPublishedJobs(query, location, user);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJobDetails(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUser") User user) {
        JobResponseDto job = jobService.getJobDetails(id, user);
        return ResponseEntity.ok(job);
    }

    @PostMapping
    public ResponseEntity<Job> createJob(
            @Valid @RequestBody JobDto jobDto,
            @RequestAttribute("authenticatedUser") User user) {
        Job job = jobService.createJob(jobDto, user);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobDto jobDto,
            @RequestAttribute("authenticatedUser") User user) {
        Job job = jobService.updateJob(id, jobDto, user);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<Job> publishJob(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUser") User user) {
        Job job = jobService.publishJob(id, user);
        return ResponseEntity.ok(job);
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<Job> closeJob(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUser") User user) {
        Job job = jobService.closeJob(id, user);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/my-jobs")
    public ResponseEntity<List<JobResponseDto>> getMyJobs(
            @RequestAttribute("authenticatedUser") User user) {
        List<JobResponseDto> jobs = jobService.getMyJobs(user);
        return ResponseEntity.ok(jobs);
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<Response> applyForJob(
            @PathVariable Long id,
            @RequestParam("resume") MultipartFile resume,
            @RequestAttribute("authenticatedUser") User user) throws IOException {
        jobService.applyForJob(id, resume, user);
        return ResponseEntity.ok(new Response("Application submitted successfully."));
    }

    @GetMapping("/my-applications")
    public ResponseEntity<List<JobApplicationResponseDto>> getMyApplications(
            @RequestAttribute("authenticatedUser") User user) {
        List<JobApplicationResponseDto> applications = jobService.getMyApplications(user);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/{id}/applicants")
    public ResponseEntity<List<JobApplicationResponseDto>> getApplicantsForJob(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUser") User user) {
        List<JobApplicationResponseDto> applicants = jobService.getApplicantsForJob(id, user);
        return ResponseEntity.ok(applicants);
    }

    @PutMapping("/applications/{applicationId}/status")
    public ResponseEntity<Response> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam("status") ApplicationStatus status,
            @RequestAttribute("authenticatedUser") User user) {
        jobService.updateApplicationStatus(applicationId, status, user);
        return ResponseEntity.ok(new Response("Application status updated to " + status.name() + "."));
    }
}
