package com.linkedin.backend.features.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Calls Google Gemini 1.5 Flash (free tier) to convert a plain-text user profile
 * into a complete, compilable LaTeX resume using the Jake's Resume template.
 *
 * <p>Free tier limits (as of 2024): 15 RPM / 1 million tokens per day.
 * Obtain an API key at https://aistudio.google.com/apikey — no billing required.
 */
@Service
public class GeminiResumeService {

    private static final Logger log = LoggerFactory.getLogger(GeminiResumeService.class);

    private static final String GEMINI_API_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    /**
     * Configurable model name — change in application.properties without recompiling.
     * Current default: gemini-2.0-flash (free tier, fast, supports system_instruction).
     */
    @Value("${gemini.model.name:gemini-2.0-flash}")
    private String geminiModelName;

    /**
     * System-level instruction sent to Gemini so it knows exactly what format to produce.
     * The model is instructed to output ONLY raw LaTeX — no markdown fences, no preamble text.
     */
    private static final String SYSTEM_PROMPT = """
            You are an expert resume writer and LaTeX typesetter.
            Given a candidate's profile information, generate a COMPLETE, COMPILABLE LaTeX resume
            using Jake's Resume template (https://www.overleaf.com/latex/templates/jakes-resume/syzfjbzwjncs).
            
            STRICT RULES:
            1. Output ONLY the raw LaTeX source code. Do NOT wrap it in markdown code fences or add any explanatory text.
            2. The document must be self-contained and compile with pdflatex without any external files.
            3. Include the full preamble (\\documentclass, \\usepackage, custom commands) and \\end{document}.
            4. Use the following Jake's Resume custom commands:
               - \\resumeSubheading{Title}{Date}{Subtitle}{Location}
               - \\resumeItem{text} inside \\resumeItemListStart ... \\resumeItemListEnd
               - \\resumeProjectHeading{\\textbf{Name} $|$ \\emph{Tech}}{Date}
               - \\resumeSubHeadingListStart ... \\resumeSubHeadingListEnd
            5. Sections to include (skip any that have no data): Summary, Skills, Experience, Education, Projects.
            6. Bullet points must start with strong action verbs and be concise (1-2 lines).
            7. Escape ALL special LaTeX characters: & % $ # _ { } ~ ^ \\ must be escaped correctly.
            8. Do NOT invent any information — use only what is provided.
            9. Contact info (name, email, location, LinkedIn-style profile) goes in the header.
            10. The Skills section should list skills as comma-separated categories, e.g.:
                \\textbf{Languages:} Java, Python $|$ \\textbf{Frameworks:} Spring Boot, React
            11. Use ONLY standard, universally installed LaTeX packages: latexsym, fullpage, titlesec, color, verbatim, enumitem, hyperref, fancyhdr, babel, tabularx. Do NOT include marvosym, fontawesome, or other non-standard symbol packages.
            12. For margins, use strictly: \\addtolength{\\oddsidemargin}{-0.5in}, \\addtolength{\\evensidemargin}{-0.5in}, \\addtolength{\\textwidth}{1in}, \\addtolength{\\topmargin}{-.5in}, \\addtolength{\\textheight}{1.0in}. NEVER invent commands like \\topbottom. All divider comments must start with % (e.g. % --------).
            """;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.resume.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiResumeService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends the profile text to Gemini and returns the raw LaTeX string.
     *
     * @param profileText Plain-text summary of the user's profile (from VirtualResumeService)
     * @return Raw LaTeX source code ready for compilation
     * @throws IllegalStateException if the API key is not configured or the call fails
     */
    public String generateLatex(String profileText) {
        if (!enabled) {
            throw new IllegalStateException("AI resume generation is currently disabled.");
        }

        String apiKey = (geminiApiKey != null) ? geminiApiKey.trim() : "";
        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key is not configured. Please set the gemini.api.key property.");
        }

        // Build URL: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
        String url = GEMINI_API_BASE + geminiModelName + ":generateContent?key=" + apiKey;
        log.info("[GeminiResume] Using model: {}", geminiModelName);

        // Build the Gemini REST payload
        // Reference: https://ai.google.dev/api/generate-content
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", SYSTEM_PROMPT))
        );

        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", profileText))
        );

        Map<String, Object> generationConfig = Map.of(
                "temperature", 0.3,          // Low temperature → deterministic, well-structured output
                "maxOutputTokens", 4096       // Enough for a full LaTeX resume
        );

        Map<String, Object> requestBody = Map.of(
                "system_instruction", systemInstruction,
                "contents", List.of(userContent),
                "generationConfig", generationConfig
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Gemini API returned an unsuccessful response: "
                        + response.getStatusCode());
            }

            return extractLatexFromResponse(response.getBody());

        } catch (IllegalStateException e) {
            throw e; // re-throw our own exceptions as-is
        } catch (Exception e) {
            log.error("[GeminiResume] API call failed: {}", e.getMessage(), e);
            throw new IllegalStateException(
                    "Failed to connect to Gemini API. Please try again later. Error: " + e.getMessage());
        }
    }

    /**
     * Parses the Gemini JSON response and extracts the generated text content.
     * Expected structure:
     * { "candidates": [{ "content": { "parts": [{ "text": "..." }] }, "finishReason": "STOP" }] }
     */
    private String extractLatexFromResponse(String responseJson) {
        try {
            log.debug("[GeminiResume] Raw response: {}", responseJson);

            JsonNode root = objectMapper.readTree(responseJson);

            // Check for API-level error block
            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText("Unknown error");
                throw new IllegalStateException("Gemini API error: " + errorMsg);
            }

            // Validate candidates array
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                log.error("[GeminiResume] No candidates found. Response: {}", responseJson);
                throw new IllegalStateException("Gemini returned no candidates in the response.");
            }

            JsonNode firstCandidate = candidates.get(0);
            if (firstCandidate == null || firstCandidate.isNull()) {
                throw new IllegalStateException("Gemini candidates[0] is null.");
            }

            // Check finish reason — SAFETY means the prompt was blocked
            String finishReason = firstCandidate.path("finishReason").asText("");
            if ("SAFETY".equals(finishReason)) {
                throw new IllegalStateException(
                        "Gemini blocked the request due to safety filters. Please check your profile content.");
            }

            // Navigate null-safely: candidate → content → parts → [0] → text
            JsonNode content = firstCandidate.path("content");
            if (content.isMissingNode() || content.isNull()) {
                log.error("[GeminiResume] 'content' missing from candidate. Candidate: {}", firstCandidate);
                throw new IllegalStateException("Gemini response is missing the 'content' field.");
            }

            JsonNode parts = content.path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                log.error("[GeminiResume] 'parts' array is missing or empty. Content: {}", content);
                throw new IllegalStateException("Gemini response 'parts' array is missing or empty.");
            }

            JsonNode firstPart = parts.get(0);
            if (firstPart == null || firstPart.isNull()) {
                throw new IllegalStateException("Gemini parts[0] is null.");
            }

            String latex = firstPart.path("text").asText(null);
            if (latex == null || latex.isBlank()) {
                log.error("[GeminiResume] 'text' is blank. Part node: {}", firstPart);
                throw new IllegalStateException("Gemini returned an empty LaTeX response.");
            }

            // Strip any accidental markdown code fences the model may have added
            latex = stripMarkdownFences(latex);

            log.info("[GeminiResume] Successfully generated LaTeX ({} chars)", latex.length());
            return latex;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GeminiResume] Failed to parse Gemini response: {}", e.getMessage());
            throw new IllegalStateException("Failed to parse Gemini response: " + e.getMessage());
        }
    }

    /**
     * Removes ```latex ... ``` or ``` ... ``` fences if the model accidentally added them.
     */
    private String stripMarkdownFences(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        // Remove opening fence: ```latex or ```
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
        }
        // Remove closing fence
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).trim();
        }
        return trimmed;
    }
}
