package com.linkedin.backend.features.resume.service;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.profile.service.VirtualResumeService;
import com.linkedin.backend.features.resume.dto.ResumeGenerationResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the end-to-end AI resume generation pipeline:
 * <ol>
 *   <li>Collects structured profile text via {@link VirtualResumeService}.</li>
 *   <li>Sends it to {@link GeminiResumeService} to get raw LaTeX back (for .tex download).</li>
 *   <li>Generates a styled PDF directly via {@link PdfResumeService} (OpenPDF — no external API).</li>
 *   <li>Saves both files to /uploads and returns their URLs.</li>
 * </ol>
 *
 * <p>Rate-limiting: users must wait {@value #RATE_LIMIT_MINUTES} minutes between generations.
 */
@Service
public class ResumeBuilderService {

    private static final Logger log = LoggerFactory.getLogger(ResumeBuilderService.class);

    /** Minimum minutes between consecutive resume generations per user. */
    private static final long RATE_LIMIT_MINUTES = 2;

    private final VirtualResumeService virtualResumeService;
    private final GeminiResumeService  geminiResumeService;
    private final PdfResumeService     pdfResumeService;

    /** Simple in-memory per-user rate limiting map. */
    private final Map<Long, LocalDateTime> lastGeneratedAt = new ConcurrentHashMap<>();

    public ResumeBuilderService(VirtualResumeService virtualResumeService,
                                GeminiResumeService  geminiResumeService,
                                PdfResumeService     pdfResumeService) {
        this.virtualResumeService = virtualResumeService;
        this.geminiResumeService  = geminiResumeService;
        this.pdfResumeService     = pdfResumeService;
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Generates an AI resume for the given authenticated user.
     *
     * @param user The authenticated user whose profile data will be used.
     * @return {@link ResumeGenerationResponseDto} with file URLs and status.
     */
    public ResumeGenerationResponseDto buildResume(User user) {
        // 1. Rate-limit check
        checkRateLimit(user);

        String uniqueId = UUID.randomUUID().toString();

        // 2. Generate PDF directly via OpenPDF (always works — no external service)
        byte[] pdfBytes   = pdfResumeService.generatePdf(user);
        String pdfFileName = "resume_" + user.getId() + "_" + uniqueId + ".pdf";
        saveBinaryFile(pdfFileName, pdfBytes);
        String pdfFileUrl = "/api/v1/storage/" + pdfFileName;

        // 3. Generate LaTeX via Gemini (for the .tex download / Overleaf editing)
        //    This is done after the PDF so a Gemini failure still gives the user the PDF.
        String texFileUrl = null;
        try {
            String profileText  = virtualResumeService.generateVirtualResumeText(user);
            String latexContent = geminiResumeService.generateLatex(profileText);
            String texFileName  = "resume_" + user.getId() + "_" + uniqueId + ".tex";
            saveTextFile(texFileName, latexContent);
            texFileUrl = "/api/v1/storage/" + texFileName;
        } catch (Exception e) {
            // Gemini failure is non-fatal — the user still gets a downloadable PDF
            log.warn("[ResumeBuilder] LaTeX generation failed for user {} (PDF still available): {}",
                    user.getId(), e.getMessage());
        }

        // 4. Record generation time for rate limiting
        lastGeneratedAt.put(user.getId(), LocalDateTime.now());

        String status  = "PDF_READY";
        String message = "Your resume has been generated successfully!";

        log.info("[ResumeBuilder] Generated resume for user {} — pdf={}, tex={}",
                user.getId(), pdfFileName, texFileUrl != null ? texFileUrl : "N/A (Gemini unavailable)");

        return new ResumeGenerationResponseDto(texFileUrl, pdfFileUrl, status, message);
    }

    // ── File Saving ─────────────────────────────────────────────────────────────

    /** Saves a text file (e.g., .tex) to the uploads directory. */
    private void saveTextFile(String fileName, String content) {
        try {
            ensureUploadsDir();
            Files.writeString(Paths.get("uploads").resolve(fileName), content);
        } catch (IOException e) {
            log.error("[ResumeBuilder] Failed to save .tex file {}: {}", fileName, e.getMessage());
            // Non-fatal — PDF is already saved
        }
    }

    /** Saves binary data (e.g., PDF bytes) to the uploads directory. */
    private void saveBinaryFile(String fileName, byte[] data) {
        try {
            ensureUploadsDir();
            Files.write(Paths.get("uploads").resolve(fileName), data);
        } catch (IOException e) {
            log.error("[ResumeBuilder] Failed to save file {}: {}", fileName, e.getMessage());
            throw new RuntimeException("Failed to save generated resume file.", e);
        }
    }

    private void ensureUploadsDir() {
        Path dir = Paths.get("uploads");
        if (!dir.toFile().exists()) dir.toFile().mkdir();
    }

    // ── Rate Limiting ───────────────────────────────────────────────────────────

    /**
     * Throws {@link TooManyRequestsException} if the user has generated a resume
     * within the last {@value #RATE_LIMIT_MINUTES} minutes.
     */
    private void checkRateLimit(User user) {
        LocalDateTime lastTime = lastGeneratedAt.get(user.getId());
        if (lastTime != null && lastTime.plusMinutes(RATE_LIMIT_MINUTES).isAfter(LocalDateTime.now())) {
            long secondsRemaining = java.time.Duration.between(
                    LocalDateTime.now(), lastTime.plusMinutes(RATE_LIMIT_MINUTES)).getSeconds();
            throw new TooManyRequestsException(
                    "Please wait " + secondsRemaining + " more seconds before generating again.");
        }
    }

    // ── Inner Exception ─────────────────────────────────────────────────────────

    /** Thrown when a user exceeds the resume generation rate limit. */
    public static class TooManyRequestsException extends RuntimeException {
        public TooManyRequestsException(String message) {
            super(message);
        }
    }
}
