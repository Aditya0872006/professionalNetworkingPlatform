package com.linkedin.backend.features.recruiter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.backend.features.recruiter.model.RecruiterProfile;
import com.linkedin.backend.features.recruiter.model.RecruiterTrustLevel;
import com.linkedin.backend.features.recruiter.model.RecruiterVerificationReport;
import com.linkedin.backend.features.recruiter.repository.RecruiterVerificationReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;

/**
 * AI-powered recruiter verification service using the Hugging Face Inference API.
 *
 * <p>Primary model: {@code facebook/bart-large-mnli} (zero-shot text classification) — free tier.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Build a plain-text summary of all recruiter profile fields.</li>
 *   <li>Send it to HuggingFace zero-shot classification with four candidate labels.</li>
 *   <li>The model returns a confidence score for each label.</li>
 *   <li>Map the top label to a {@link RecruiterTrustLevel}.</li>
 *   <li>Run lightweight rule-based sub-scorers (email, website, company, location,
 *       consistency) to compute per-dimension scores for admin visibility.</li>
 *   <li>Persist a {@link RecruiterVerificationReport} — the admin dashboard reads it.</li>
 * </ol>
 *
 * <p>The call is {@code @Async} so recruiter signup is never slowed down.
 */
@Service
public class HuggingFaceVerificationService {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceVerificationService.class);

    // ── HuggingFace ───────────────────────────────────────────────────────────
    private static final String HF_API_URL =
            "https://api-inference.huggingface.co/models/facebook/bart-large-mnli";

    private static final List<String> CANDIDATE_LABELS = List.of(
            "legitimate company", "suspicious profile", "fake company", "spam registration"
    );

    // ── Rule-based helpers ────────────────────────────────────────────────────
    private static final Set<String> FREE_EMAIL_DOMAINS = Set.of(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "icloud.com", "protonmail.com", "aol.com", "mail.com",
            "ymail.com", "live.com", "rediffmail.com", "inbox.com"
    );

    private static final Set<String> SUSPICIOUS_TLDS = Set.of(
            ".xyz", ".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".click", ".pw"
    );

    private static final Set<String> PLACEHOLDER_NAMES = Set.of(
            "test", "my company", "abc", "company", "xyz", "demo", "sample",
            "best jobs", "jobs company", "hiring company", "your company"
    );

    private static final Pattern RANDOM_CHARS = Pattern.compile("[^a-zA-Z\\s]{3,}");
    private static final Pattern IP_URL       = Pattern.compile("https?://\\d+\\.\\d+\\.\\d+\\.\\d+");

    // ── Injections ─────────────────────────────────────────────────────────────
    @Value("${huggingface.api.token:}")
    private String hfToken;

    @Value("${recruiter.ai.verification.enabled:true}")
    private boolean enabled;

    private final RecruiterVerificationReportRepository reportRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HuggingFaceVerificationService(
            RecruiterVerificationReportRepository reportRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.restTemplate     = restTemplate;
        this.objectMapper     = objectMapper;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Runs AI verification asynchronously — does NOT block the signup thread.
     * Safe to call and forget; any exception is caught and logged.
     */
    @Async
    public void analyzeAsync(RecruiterProfile profile) {
        if (!enabled) {
            log.info("[AI-Verify] Disabled by config — skipping profile {}", profile.getId());
            return;
        }

        // Delete any previous report (re-analysis on reapply)
        reportRepository.findByRecruiterProfileId(profile.getId())
                .ifPresent(reportRepository::delete);

        try {
            RecruiterVerificationReport report = analyze(profile);
            reportRepository.save(report);
            log.info("[AI-Verify] Profile {} → {} (score={})",
                    profile.getId(), report.getTrustLevel(), report.getTrustScore());
        } catch (Exception ex) {
            log.error("[AI-Verify] Failed for profile {}: {}", profile.getId(), ex.getMessage(), ex);
            // Graceful degradation: admin can still review manually without the AI badge
        }
    }

    // ── Core Analysis ──────────────────────────────────────────────────────────

    private RecruiterVerificationReport analyze(RecruiterProfile profile) {
        List<String> flags = new ArrayList<>();

        // ─ Sub-scorers (each returns 0..maxPts) ──────────────────────────────
        int emailScore       = scoreEmail(profile, flags);
        int websiteScore     = scoreWebsite(profile, flags);
        int companyScore     = scoreCompanyName(profile, flags);
        int locationScore    = scoreLocation(profile, flags);
        int consistencyScore = scoreConsistency(profile, flags);

        int ruleTotal = emailScore + websiteScore + companyScore + locationScore + consistencyScore; // 0-100

        // ─ HuggingFace zero-shot classification ───────────────────────────────
        HFResult hfResult = callHuggingFace(profile);

        // ─ Blend: 40% rules + 60% HF model ───────────────────────────────────
        // If HF model was unavailable (modelScore == -1), use 100% rule score
        int finalScore;
        boolean hfAvailable = hfResult.modelScore >= 0;
        if (hfAvailable) {
            finalScore = (int) Math.round(0.40 * ruleTotal + 0.60 * hfResult.modelScore);
        } else {
            finalScore = ruleTotal; // 100% rule-based when HF unavailable
        }
        finalScore = Math.max(0, Math.min(100, finalScore));

        // Note: do NOT add hfResult.modelFlag to the recruiter's flags —
        // system status messages are not red flags about the recruiter.

        RecruiterTrustLevel trustLevel = scoreToLevel(finalScore);

        // ─ Build reasoning ────────────────────────────────────────────────────
        String reasoning = buildReasoning(profile, trustLevel, finalScore, hfResult, flags);

        // ─ Assemble report ────────────────────────────────────────────────────
        RecruiterVerificationReport report = new RecruiterVerificationReport();
        report.setRecruiterProfile(profile);
        report.setTrustLevel(trustLevel);
        report.setTrustScore(finalScore);
        report.setEmailScore(emailScore);
        report.setWebsiteScore(websiteScore);
        report.setCompanyScore(companyScore);
        report.setLocationScore(locationScore);
        report.setConsistencyScore(consistencyScore);
        report.setFlagList(flags);
        report.setReasoning(reasoning);
        report.setModelUsed(hfAvailable
                ? "HuggingFace bart-large-mnli (zero-shot, 60%) + rule engine (40%)"
                : "Rule-based analysis only (HuggingFace model unavailable)");
        return report;
    }

    // ── Sub-scorers ───────────────────────────────────────────────────────────

    /** Max 25 pts */
    private int scoreEmail(RecruiterProfile p, List<String> flags) {
        String email = p.getCompanyEmail();
        if (email == null || email.isBlank()) {
            flags.add("No company email provided");
            return 0;
        }
        int score = 25;
        String domain = extractDomain(email);
        if (FREE_EMAIL_DOMAINS.contains(domain.toLowerCase())) {
            flags.add("Free email provider used for company email (" + domain + ")");
            score -= 15;
        }
        // Generic/personal pattern: digits in local part, e.g. john123@
        String localPart = email.contains("@") ? email.split("@")[0] : "";
        if (localPart.matches(".*\\d{3,}.*")) {
            flags.add("Company email looks like a personal email (many digits in local part)");
            score -= 5;
        }
        return Math.max(0, score);
    }

    /** Max 20 pts */
    private int scoreWebsite(RecruiterProfile p, List<String> flags) {
        String url = p.getCompanyWebsite();
        if (url == null || url.isBlank()) {
            flags.add("No company website provided");
            return 0;
        }
        int score = 20;
        if (!url.startsWith("https://")) {
            flags.add("Company website does not use HTTPS");
            score -= 8;
        }
        if (IP_URL.matcher(url).matches()) {
            flags.add("Company website is an IP address (not a real domain)");
            score -= 15;
        }
        if (url.length() < 12) {
            flags.add("Company website URL is suspiciously short");
            score -= 8;
        }
        for (String tld : SUSPICIOUS_TLDS) {
            if (url.contains(tld)) {
                flags.add("Company website uses a suspicious TLD (" + tld + ")");
                score -= 10;
                break;
            }
        }
        return Math.max(0, score);
    }

    /** Max 20 pts */
    private int scoreCompanyName(RecruiterProfile p, List<String> flags) {
        String name = p.getCompanyName();
        if (name == null || name.isBlank()) {
            flags.add("Company name is missing");
            return 0;
        }
        int score = 20;
        if (name.trim().length() < 3) {
            flags.add("Company name is too short");
            score -= 15;
        }
        if (PLACEHOLDER_NAMES.contains(name.trim().toLowerCase())) {
            flags.add("Company name appears to be a placeholder (\"" + name + "\")");
            score -= 15;
        }
        if (name.matches("\\d+")) {
            flags.add("Company name contains only numbers");
            score -= 15;
        }
        if (RANDOM_CHARS.matcher(name).find()) {
            flags.add("Company name contains unusual characters");
            score -= 8;
        }
        // Bonus: contains real business suffix
        String lower = name.toLowerCase();
        if (lower.contains(" inc") || lower.contains(" ltd") || lower.contains(" llc")
                || lower.contains(" pvt") || lower.contains(" corp") || lower.contains(" gmbh")
                || lower.contains(" bpo") || lower.contains(" solutions") || lower.contains(" technologies")) {
            score = Math.min(20, score + 5);
        }
        return Math.max(0, score);
    }

    /** Max 15 pts */
    private int scoreLocation(RecruiterProfile p, List<String> flags) {
        String loc = p.getCompanyLocation();
        if (loc == null || loc.isBlank()) {
            flags.add("No company location provided");
            return 0;
        }
        int score = 15;
        if (loc.trim().length() < 3) {
            flags.add("Company location is too short to be valid");
            score -= 10;
        }
        if (!loc.contains(",") && !loc.contains(" ")) {
            flags.add("Company location missing city/country pattern (e.g. 'Mumbai, India')");
            score -= 5;
        }
        if (loc.matches("\\d+")) {
            flags.add("Company location contains only numbers");
            score -= 12;
        }
        return Math.max(0, score);
    }

    /** Max 20 pts */
    private int scoreConsistency(RecruiterProfile p, List<String> flags) {
        int score = 10; // base: some fields exist

        String email   = p.getCompanyEmail();
        String website = p.getCompanyWebsite();
        String name    = p.getCompanyName();

        // All key fields filled
        boolean allFilled = email != null && !email.isBlank()
                && website != null && !website.isBlank()
                && p.getCompanyLocation() != null && !p.getCompanyLocation().isBlank()
                && p.getCompanyDescription() != null && !p.getCompanyDescription().isBlank();
        if (allFilled) score += 5;

        // Email domain matches website domain
        if (email != null && website != null && !email.isBlank() && !website.isBlank()) {
            String emailDomain   = extractDomain(email).toLowerCase();
            String websiteDomain = extractWebsiteDomain(website).toLowerCase();
            if (!emailDomain.isBlank() && !websiteDomain.isBlank()
                    && emailDomain.equals(websiteDomain)) {
                score += 5; // domains match → very strong signal
            } else if (!emailDomain.isBlank() && !websiteDomain.isBlank()) {
                flags.add("Company email domain (" + emailDomain
                        + ") does not match website domain (" + websiteDomain + ")");
                score -= 8;
            }
        }

        // Company name keyword present in email or website
        if (name != null && !name.isBlank()) {
            String nameLower = name.toLowerCase().replaceAll("[^a-z]", "");
            String emailLower   = email   != null ? email.toLowerCase()   : "";
            String websiteLower = website != null ? website.toLowerCase() : "";
            boolean nameInEmail   = nameLower.length() > 2 && emailLower.contains(nameLower.substring(0, Math.min(4, nameLower.length())));
            boolean nameInWebsite = nameLower.length() > 2 && websiteLower.contains(nameLower.substring(0, Math.min(4, nameLower.length())));
            if (!nameInEmail && !nameInWebsite && emailLower.length() > 0) {
                flags.add("Company name not reflected in email or website URL");
                score -= 5;
            }
        }
        return Math.max(0, Math.min(20, score));
    }

    // ── HuggingFace Call ──────────────────────────────────────────────────────

    private HFResult callHuggingFace(RecruiterProfile p) {
        if (hfToken == null || hfToken.isBlank()) {
            log.info("[AI-Verify] HuggingFace token not configured — using 100% rule-based score");
            return new HFResult(-1, ""); // -1 signals: use rule score only
        }

        try {
            String inputText = buildInputText(p);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("inputs", inputText);
            body.put("parameters", Map.of("candidate_labels", CANDIDATE_LABELS));
            // wait_for_model: true tells HF to wait for cold-start instead of returning 503
            body.put("options", Map.of("wait_for_model", true));

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + hfToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    HF_API_URL, request, String.class);

            return parseHFResponse(response.getBody());
        } catch (Exception ex) {
            log.warn("[AI-Verify] HuggingFace API call failed: {} — using 100% rule-based score", ex.getMessage());
            return new HFResult(-1, ""); // -1 signals: use rule score only
        }
    }

    private HFResult parseHFResponse(String json) throws Exception {
        JsonNode root   = objectMapper.readTree(json);
        JsonNode labels = root.get("labels");
        JsonNode scores = root.get("scores");

        if (labels == null || scores == null || labels.isEmpty()) {
            return new HFResult(-1, ""); // invalid response — fall back to rules
        }

        // The label with the highest confidence is at index 0
        String topLabel = labels.get(0).asText();
        double topScore = scores.get(0).asDouble();

        // Map model label to a 0-100 trust score
        int modelScore = switch (topLabel) {
            case "legitimate company"  -> (int) (50 + topScore * 50);  // 50-100
            case "suspicious profile"  -> (int) (25 + topScore * 30);  // 25-55
            case "fake company"        -> (int) (10 + topScore * 30);  // 10-40
            case "spam registration"   -> (int) (topScore * 20);       // 0-20
            default                    -> 50;
        };
        modelScore = Math.max(0, Math.min(100, modelScore));

        String modelFlag = "";
        if (!"legitimate company".equals(topLabel) && topScore > 0.55) {
            String label = topLabel.substring(0, 1).toUpperCase() + topLabel.substring(1);
            modelFlag = "🤖 AI model flagged as: " + label
                    + " (confidence: " + String.format("%.0f", topScore * 100) + "%)";
        }

        return new HFResult(modelScore, modelFlag);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildInputText(RecruiterProfile p) {
        return "Recruiter application details:\n"
                + "Company Name: " + orNA(p.getCompanyName()) + "\n"
                + "Company Email: " + orNA(p.getCompanyEmail()) + "\n"
                + "Company Website: " + orNA(p.getCompanyWebsite()) + "\n"
                + "Company Location: " + orNA(p.getCompanyLocation()) + "\n"
                + "Company Description: " + orNA(p.getCompanyDescription()) + "\n"
                + "Recruiter Name: " + orNA(p.getUser().getFirstName()) + " " + orNA(p.getUser().getLastName()) + "\n"
                + "Recruiter Email: " + orNA(p.getUser().getEmail());
    }

    private String buildReasoning(RecruiterProfile p, RecruiterTrustLevel level,
                                  int score, HFResult hf, List<String> flags) {
        StringBuilder sb = new StringBuilder();
        sb.append("AI analysis for ").append(p.getCompanyName()).append(": ");
        sb.append("Overall trust score is ").append(score).append("/100 (").append(level).append("). ");

        if (!hf.modelFlag.isBlank()) {
            sb.append(hf.modelFlag).append(". ");
        }

        if (flags.isEmpty()) {
            sb.append("No major red flags were detected. Profile appears consistent and legitimate.");
        } else {
            sb.append("Detected ").append(flags.size()).append(" issue(s): ")
              .append(String.join("; ", flags)).append(".");
        }

        sb.append(" Admin recommendation: ");
        sb.append(switch (level) {
            case LEGITIMATE   -> "Profile looks legitimate — consider approving.";
            case SUSPICIOUS   -> "Review profile carefully before approving.";
            case LIKELY_FAKE  -> "Profile shows multiple red flags — strong caution advised.";
            case HIGH_RISK    -> "Profile is likely fraudulent — consider rejecting.";
        });

        return sb.toString();
    }

    private static RecruiterTrustLevel scoreToLevel(int score) {
        if (score >= 80) return RecruiterTrustLevel.LEGITIMATE;
        if (score >= 50) return RecruiterTrustLevel.SUSPICIOUS;
        if (score >= 25) return RecruiterTrustLevel.LIKELY_FAKE;
        return RecruiterTrustLevel.HIGH_RISK;
    }

    private static String extractDomain(String email) {
        if (email == null || !email.contains("@")) return "";
        String[] parts = email.split("@");
        return parts.length > 1 ? parts[1].trim() : "";
    }

    private static String extractWebsiteDomain(String url) {
        if (url == null || url.isBlank()) return "";
        String domain = url.replaceFirst("https?://", "").replaceFirst("www\\.", "");
        int slash = domain.indexOf('/');
        if (slash > 0) domain = domain.substring(0, slash);
        return domain.trim();
    }

    private static String orNA(String s) {
        return (s == null || s.isBlank()) ? "N/A" : s;
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    /** Holds the HuggingFace model's translated trust score and a human-readable flag. */
    private record HFResult(int modelScore, String modelFlag) {}
}
