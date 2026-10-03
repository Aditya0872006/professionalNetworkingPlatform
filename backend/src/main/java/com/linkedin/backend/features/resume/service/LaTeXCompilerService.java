package com.linkedin.backend.features.resume.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Compiles a LaTeX (.tex) source string to a PDF using the locally-installed
 * {@code pdflatex} binary (MiKTeX or TeXLive).
 *
 * <p>This produces output identical to Overleaf because both use the same
 * pdflatex/TeXLive engine under the hood.
 *
 * <p>If {@code pdflatex} is not on the system PATH (or compilation fails),
 * {@link #compile(String)} returns {@link Optional#empty()} and the caller
 * should fall back to another strategy.
 */
@Service
public class LaTeXCompilerService {

    private static final Logger log = LoggerFactory.getLogger(LaTeXCompilerService.class);

    /** Seconds to wait for pdflatex before killing the process. */
    private static final int TIMEOUT_SECONDS = 120;

    /**
     * Compiles the given LaTeX source to a PDF.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Sanitize common LLM LaTeX output errors.</li>
     *   <li>Write the .tex to a temp directory.</li>
     *   <li>Run {@code pdflatex -enable-installer -interaction=nonstopmode} twice
     *       (second pass resolves cross-references).</li>
     *   <li>Read the resulting .pdf bytes and return them.</li>
     *   <li>Clean up the temp directory regardless of outcome.</li>
     * </ol>
     *
     * @param latexSource Raw LaTeX source (the full .tex file content)
     * @return PDF bytes wrapped in Optional, or empty if compilation failed
     */
    public Optional<byte[]> compile(String latexSource) {
        latexSource = sanitizeLatex(latexSource);
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("resume_latex_");
            Path texFile = tempDir.resolve("resume.tex");
            Files.writeString(texFile, latexSource, StandardCharsets.UTF_8);

            // Run pdflatex twice — first pass builds the structure,
            // second pass resolves any page/section references in the .tex
            boolean firstPass = runPdflatex(texFile, tempDir);
            if (!firstPass) {
                log.warn("[LaTeXCompiler] First pdflatex pass failed.");
                return Optional.empty();
            }
            runPdflatex(texFile, tempDir); // second pass — ignore exit code

            Path pdfFile = tempDir.resolve("resume.pdf");
            if (Files.exists(pdfFile) && Files.size(pdfFile) > 0) {
                byte[] pdfBytes = Files.readAllBytes(pdfFile);
                log.info("[LaTeXCompiler] PDF compiled successfully ({} bytes)", pdfBytes.length);
                return Optional.of(pdfBytes);
            }

            log.warn("[LaTeXCompiler] pdflatex ran but produced no PDF.");
            return Optional.empty();

        } catch (IOException e) {
            log.warn("[LaTeXCompiler] I/O error during LaTeX compilation: {}", e.getMessage());
            return Optional.empty();
        } finally {
            cleanUp(tempDir);
        }
    }

    /**
     * Sanitizes known LLM hallucinations in LaTeX output before compilation.
     */
    public String sanitizeLatex(String latex) {
        if (latex == null) return "";

        // Fix hallucinated \topbottom -> \topmargin
        latex = latex.replace("\\topbottom", "\\topmargin");

        // Fix raw dashed divider lines missing % comment symbol (e.g. "----------------------")
        latex = latex.replaceAll("(?m)^\\s*-{3,}\\s*$", "% -------------------------------------------");

        return latex;
    }

    /**
     * Resolves the pdflatex binary path, prioritizing known MiKTeX installation locations
     * if not already present on system PATH.
     */
    private String resolvePdflatexExecutable() {
        // 1. Check user AppData MiKTeX location (common on Windows)
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Path userMiktex = Paths.get(userHome, "AppData", "Local", "Programs", "MiKTeX", "miktex", "bin", "x64", "pdflatex.exe");
            if (Files.exists(userMiktex)) {
                return userMiktex.toAbsolutePath().toString();
            }
        }

        // 2. Check Program Files MiKTeX location
        Path sysMiktex = Paths.get("C:", "Program Files", "MiKTeX", "miktex", "bin", "x64", "pdflatex.exe");
        if (Files.exists(sysMiktex)) {
            return sysMiktex.toAbsolutePath().toString();
        }

        // 3. Fallback to standard PATH command
        return "pdflatex";
    }

    /**
     * Runs a single {@code pdflatex} pass on the given .tex file.
     * Uses only the bare filename (not the full path) so Windows paths
     * containing spaces do not confuse pdflatex.
     *
     * @return {@code true} if pdflatex exited with code 0 within the timeout
     */
    private boolean runPdflatex(Path texFile, Path workDir) {
        String pdflatexCmd = resolvePdflatexExecutable();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pdflatexCmd,
                    "-enable-installer",          // auto-install any missing MiKTeX packages without prompting
                    "-interaction=nonstopmode",   // never pause for user input
                    "-halt-on-error",             // stop on first error
                    texFile.getFileName().toString()  // ← bare filename only (e.g. "resume.tex")
                    // output goes to workDir because that is the working directory
            );
            // Set CWD to the temp dir — pdflatex writes .pdf, .log, .aux here
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);         // merge stderr into stdout

            Process process = pb.start();

            // Drain stdout/stderr to prevent buffer-full deadlock
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("[LaTeXCompiler] pdflatex timed out after {}s", TIMEOUT_SECONDS);
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // Log the last ~60 lines of pdflatex output which contain the actual error
                String[] lines = output.split("\n");
                int from = Math.max(0, lines.length - 60);
                StringBuilder tail = new StringBuilder();
                for (int i = from; i < lines.length; i++) tail.append(lines[i]).append("\n");
                log.warn("[LaTeXCompiler] pdflatex exited {}. Last output:\n{}", exitCode, tail);
                return false;
            }

            return true;

        } catch (IOException e) {
            // pdflatex not found on PATH
            log.warn("[LaTeXCompiler] pdflatex not found or could not start: {}. " +
                     "Install MiKTeX (https://miktex.org) or TeXLive to enable LaTeX compilation.", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[LaTeXCompiler] pdflatex interrupted.");
            return false;
        }
    }

    /** Recursively deletes the temp directory and all its contents. */
    private void cleanUp(Path dir) {
        if (dir == null) return;
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.debug("[LaTeXCompiler] Could not clean up temp dir {}: {}", dir, e.getMessage());
        }
    }
}
