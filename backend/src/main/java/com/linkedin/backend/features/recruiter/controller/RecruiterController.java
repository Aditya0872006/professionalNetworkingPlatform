package com.linkedin.backend.features.recruiter.controller;

import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.recruiter.model.RecruiterProfile;
import com.linkedin.backend.features.recruiter.service.RecruiterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/recruiter")
public class RecruiterController {

    private final RecruiterService recruiterService;

    public RecruiterController(RecruiterService recruiterService) {
        this.recruiterService = recruiterService;
    }

    @GetMapping("/profile/me")
    public ResponseEntity<RecruiterProfile> getMyRecruiterProfile(@RequestAttribute("authenticatedUser") User user) {
        if (user.getRole() != Role.ROLE_RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters have a recruiter profile.");
        }
        RecruiterProfile profile = recruiterService.getProfileByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recruiter profile not found."));
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/reapply")
    public ResponseEntity<Map<String, Object>> reapply(@RequestAttribute("authenticatedUser") User user) {
        if (user.getRole() != Role.ROLE_RECRUITER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiters can reapply.");
        }
        RecruiterProfile profile = recruiterService.reapply(user.getId());
        return ResponseEntity.ok(Map.of(
                "message", "Your application has been resubmitted for review.",
                "status", profile.getStatus().name(),
                "rejectionCount", profile.getRejectionCount()
        ));
    }
}
