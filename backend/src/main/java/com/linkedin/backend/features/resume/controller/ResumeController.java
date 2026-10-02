package com.linkedin.backend.features.resume.controller;

import com.linkedin.backend.dto.Response;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.resume.dto.ResumeGenerationResponseDto;
import com.linkedin.backend.features.resume.service.ResumeBuilderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI-powered resume generation.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/resume/generate — Generate a resume for the authenticated user.</li>
 * </ul>
 *
 * File download is handled by the existing StorageController at /api/v1/storage/{filename}.
 */
@RestController
@RequestMapping("/api/v1/resume")
public class ResumeController {

    private final ResumeBuilderService resumeBuilderService;

    public ResumeController(ResumeBuilderService resumeBuilderService) {
        this.resumeBuilderService = resumeBuilderService;
    }

    /**
     * Generates an AI resume for the authenticated user based on their profile data.
     *
     * <p>Returns a {@link ResumeGenerationResponseDto} containing:
     * <ul>
     *   <li>{@code texFileUrl}  — always present; URL to download the raw .tex file.</li>
     *   <li>{@code pdfFileUrl}  — present only if PDF compilation succeeded.</li>
     *   <li>{@code status}      — "PDF_READY" or "TEX_ONLY".</li>
     *   <li>{@code message}     — human-readable description.</li>
     * </ul>
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateResume(
            @RequestAttribute("authenticatedUser") User user) {
        try {
            ResumeGenerationResponseDto result = resumeBuilderService.buildResume(user);
            return ResponseEntity.ok(result);

        } catch (ResumeBuilderService.TooManyRequestsException e) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new Response(e.getMessage()));

        } catch (IllegalStateException e) {
            // Covers: missing API key, Gemini errors, safety blocks
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Response(e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Response("An unexpected error occurred while generating your resume. Please try again."));
        }
    }
}
