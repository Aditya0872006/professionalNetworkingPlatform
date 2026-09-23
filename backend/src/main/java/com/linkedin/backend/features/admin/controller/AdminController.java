package com.linkedin.backend.features.admin.controller;

import com.linkedin.backend.dto.Response;
import com.linkedin.backend.features.admin.dto.AdminStatsDto;
import com.linkedin.backend.features.admin.service.AdminService;
import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.authentication.model.UserStatus;
import com.linkedin.backend.features.feed.model.Post;
import com.linkedin.backend.features.jobs.model.Job;
import com.linkedin.backend.features.recruiter.model.RecruiterProfile;
import com.linkedin.backend.features.recruiter.model.RecruiterStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getStats(@RequestAttribute("authenticatedUser") User admin) {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(value = "role", required = false) Role role,
            @RequestParam(value = "status", required = false) UserStatus status,
            @RequestAttribute("authenticatedUser") User admin) {
        return ResponseEntity.ok(adminService.getAllUsers(role, status));
    }

    @PutMapping("/users/{userId}/block")
    public ResponseEntity<Response> blockUser(
            @PathVariable Long userId,
            @RequestAttribute("authenticatedUser") User admin) {
        adminService.blockUser(userId, admin);
        return ResponseEntity.ok(new Response("User has been blocked successfully."));
    }

    @PutMapping("/users/{userId}/unblock")
    public ResponseEntity<Response> unblockUser(
            @PathVariable Long userId,
            @RequestAttribute("authenticatedUser") User admin) {
        adminService.unblockUser(userId, admin);
        return ResponseEntity.ok(new Response("User has been unblocked successfully."));
    }

    @GetMapping("/recruiters")
    public ResponseEntity<List<RecruiterProfile>> getRecruiterApplications(
            @RequestParam(value = "status", required = false) RecruiterStatus status,
            @RequestAttribute("authenticatedUser") User admin) {
        return ResponseEntity.ok(adminService.getRecruiterApplications(status));
    }

    @PutMapping("/recruiters/{id}/approve")
    public ResponseEntity<Response> approveRecruiter(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUser") User admin) {
        adminService.approveRecruiter(id, admin);
        return ResponseEntity.ok(new Response("Recruiter application approved successfully."));
    }

    @PutMapping("/recruiters/{id}/reject")
    public ResponseEntity<Response> rejectRecruiter(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUser") User admin) {
        adminService.rejectRecruiter(id, admin);
        return ResponseEntity.ok(new Response("Recruiter application rejected."));
    }

    @GetMapping("/posts")
    public ResponseEntity<List<Post>> getAllPosts(@RequestAttribute("authenticatedUser") User admin) {
        return ResponseEntity.ok(adminService.getAllPosts());
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Response> deletePost(
            @PathVariable Long postId,
            @RequestAttribute("authenticatedUser") User admin) {
        adminService.deletePost(postId, admin);
        return ResponseEntity.ok(new Response("Post removed by administrator."));
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getAllJobs(@RequestAttribute("authenticatedUser") User admin) {
        return ResponseEntity.ok(adminService.getAllJobs());
    }

    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<Response> removeJob(
            @PathVariable Long jobId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestAttribute("authenticatedUser") User admin) {
        adminService.removeJob(jobId, reason, admin);
        return ResponseEntity.ok(new Response("Job removed by administrator."));
    }
}
