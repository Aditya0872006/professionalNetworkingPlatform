package com.linkedin.backend.features.resume.service;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.profile.service.VirtualResumeService;
import com.linkedin.backend.features.resume.dto.ResumeGenerationResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the end-to-end AI resume generation pipeline:
 *
 * <ol>
 *   <li>Rate-limit check.</li>
 *   <li>Generate LaTeX via {@link GeminiResumeService} (Google Gemini AI).</li>
 *   <li>Save the raw .tex file to /uploads.</li>
 *   <li>Attempt PDF compilation via {@link LaTeXCompilerService} (local pdflatex —
 *       same engine as Overleaf → identical output).</li>
 *   <li>If pdflatex is unavailable, fall back to {@link PdfResumeService} (OpenPDF).</li>
 *   <li>Return file URLs and status to the controller.</li>
 * </ol>
 *
 * <p>Rate-limiting: users must wait {@value #RATE_LIMIT_MINUTES} minutes between generations.
 */
@Service
public class ResumeBuilderService {

    private static final Logger log = LoggerFactory.getLogger(ResumeBuilderService.class);

    /** Minimum minutes between consecutive resume generations per user. */
    private static final long RATE_LIMIT_MINUTES = 2;

    private final VirtualResumeService  virtualResumeService;
    private final GeminiResumeService   geminiResumeService;
    private final LaTeXCompilerService  laTeXCompilerService;
    private final PdfResumeService      pdfResumeService;

    /** Simple in-memory per-user rate limiting map. */
    private final Map<Long, LocalDateTime> lastGeneratedAt = new ConcurrentHashMap<>();

    public ResumeBuilderService(VirtualResumeService  virtualResumeService,
                                GeminiResumeService   geminiResumeService,
                                LaTeXCompilerService  laTeXCompilerService,
                                PdfResumeService      pdfResumeService) {
        this.virtualResumeService = virtualResumeService;
        this.geminiResumeService  = geminiResumeService;
        this.laTeXCompilerService = laTeXCompilerService;
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

        // 2. Generate LaTeX via Gemini AI
        String profileText  = virtualResumeService.generateVirtualResumeText(user);
        String latexContent = geminiResumeService.generateLatex(profileText);
        latexContent = laTeXCompilerService.sanitizeLatex(latexContent);

        // 3. Save .tex file (always — user can download and compile on Overleaf)
        String texFileName = "resume_" + user.getId() + "_" + uniqueId + ".tex";
        saveTextFile(texFileName, latexContent);
        String texFileUrl = "/api/v1/storage/" + texFileName;

        // 4. Compile to PDF — primary: local pdflatex (Overleaf-identical output)
        //    fallback: OpenPDF (guaranteed, but simpler styling)
        byte[]  pdfBytes;
        String  pdfSource;
        Optional<byte[]> compiled = laTeXCompilerService.compile(latexContent);

        if (compiled.isPresent()) {
            pdfBytes  = compiled.get();
            pdfSource = "pdflatex";
        } else {
            log.info("[ResumeBuilder] pdflatex unavailable — falling back to OpenPDF for user {}", user.getId());
            pdfBytes  = pdfResumeService.generatePdf(user);
            pdfSource = "openpdf-fallback";
        }

        // 5. Save PDF
        String pdfFileName = "resume_" + user.getId() + "_" + uniqueId + ".pdf";
        saveBinaryFile(pdfFileName, pdfBytes);
        String pdfFileUrl = "/api/v1/storage/" + pdfFileName;

        // 6. Record generation time for rate limiting
        lastGeneratedAt.put(user.getId(), LocalDateTime.now());

        log.info("[ResumeBuilder] Resume generated for user {} — source={}, tex={}, pdf={}",
                user.getId(), pdfSource, texFileName, pdfFileName);

        return new ResumeGenerationResponseDto(
                texFileUrl,
                pdfFileUrl,
                "PDF_READY",
                "PDF_READY".equals("PDF_READY") && "pdflatex".equals(pdfSource)
                        ? "Your resume has been compiled with pdflatex — identical to Overleaf output!"
                        : "Your resume has been generated successfully!"
        );
    }

    /**
     * Retrieves the most recently generated resume for the user from the uploads directory.
     *
     * @param user The authenticated user
     * @return Optional containing ResumeGenerationResponseDto if found, or empty if no resume exists yet
     */
    public Optional<ResumeGenerationResponseDto> getLatestResume(User user) {
        Path uploadsDir = Paths.get("uploads");
        if (!Files.exists(uploadsDir) || !Files.isDirectory(uploadsDir)) {
            return Optional.empty();
        }

        String prefix = "resume_" + user.getId() + "_";

        try (var stream = Files.list(uploadsDir)) {
            List<Path> userFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();

            Optional<Path> latestPdf = userFiles.stream()
                    .filter(p -> p.getFileName().toString().endsWith(".pdf"))
                    .findFirst();

            Optional<Path> latestTex = userFiles.stream()
                    .filter(p -> p.getFileName().toString().endsWith(".tex"))
                    .findFirst();

            if (latestPdf.isEmpty() && latestTex.isEmpty()) {
                return Optional.empty();
            }

            String pdfFileUrl = latestPdf.map(p -> "/api/v1/storage/" + p.getFileName().toString()).orElse(null);
            String texFileUrl = latestTex.map(p -> "/api/v1/storage/" + p.getFileName().toString()).orElse(null);
            String status = pdfFileUrl != null ? "PDF_READY" : "TEX_ONLY";

            return Optional.of(new ResumeGenerationResponseDto(
                    texFileUrl,
                    pdfFileUrl,
                    status,
                    "Previously generated resume retrieved."
            ));
        } catch (IOException e) {
            log.error("[ResumeBuilder] Error reading uploads directory for user {}: {}", user.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    // ── File Saving ─────────────────────────────────────────────────────────────

    /** Saves LaTeX source text to the uploads directory. */
    private void saveTextFile(String fileName, String content) {
        try {
            ensureUploadsDir();
            Files.writeString(Paths.get("uploads").resolve(fileName), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[ResumeBuilder] Failed to save .tex file {}: {}", fileName, e.getMessage());
            // Non-fatal — PDF still gets generated
        }
    }

    /** Saves binary (PDF) data to the uploads directory. */
    private void saveBinaryFile(String fileName, byte[] data) {
        try {
            ensureUploadsDir();
            Files.write(Paths.get("uploads").resolve(fileName), data);
        } catch (IOException e) {
            log.error("[ResumeBuilder] Failed to save .pdf file {}: {}", fileName, e.getMessage());
            throw new RuntimeException("Failed to save generated resume PDF.", e);
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
