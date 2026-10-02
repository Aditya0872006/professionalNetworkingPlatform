package com.linkedin.backend.features.resume.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.profile.model.Education;
import com.linkedin.backend.features.profile.model.Experience;
import com.linkedin.backend.features.profile.model.Project;
import com.linkedin.backend.features.profile.model.UserSkill;
import com.linkedin.backend.features.profile.repository.EducationRepository;
import com.linkedin.backend.features.profile.repository.ExperienceRepository;
import com.linkedin.backend.features.profile.repository.ProjectRepository;
import com.linkedin.backend.features.profile.repository.UserSkillRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates a clean, ATS-friendly PDF resume directly from the user's profile
 * using OpenPDF (free open-source library) — no external API, no LaTeX required.
 */
@Service
public class PdfResumeService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final Color HEADER_COLOR    = new Color(10, 102, 194);   // LinkedIn blue
    private static final Color DIVIDER_COLOR   = new Color(10, 102, 194);
    private static final Color SECTION_COLOR   = new Color(30, 30, 30);
    private static final Color BODY_COLOR      = new Color(50, 50, 50);
    private static final Color SUBTLE_COLOR    = new Color(100, 100, 100);

    private final UserSkillRepository   skillRepo;
    private final ExperienceRepository  expRepo;
    private final EducationRepository   eduRepo;
    private final ProjectRepository     projRepo;

    public PdfResumeService(UserSkillRepository skillRepo,
                            ExperienceRepository expRepo,
                            EducationRepository eduRepo,
                            ProjectRepository projRepo) {
        this.skillRepo = skillRepo;
        this.expRepo   = expRepo;
        this.eduRepo   = eduRepo;
        this.projRepo  = projRepo;
    }

    /**
     * Builds the PDF in memory and returns the raw bytes.
     * Throws {@link RuntimeException} if generation fails (extremely unlikely).
     */
    public byte[] generatePdf(User user) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document doc = new Document(PageSize.A4, 50, 50, 45, 45);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addContent(doc, user);

        doc.close();
        return out.toByteArray();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private void addContent(Document doc, User user) {
        try {
            addHeader(doc, user);

            List<UserSkill>  skills      = skillRepo.findByUserIdOrderByIdAsc(user.getId());
            List<Experience> experiences = expRepo.findByUserIdOrderByStartDateDesc(user.getId());
            List<Education>  educations  = eduRepo.findByUserIdOrderByStartDateDesc(user.getId());
            List<Project>    projects    = projRepo.findByUserIdOrderByIdDesc(user.getId());

            if (user.getAbout() != null && !user.getAbout().isBlank()) {
                addSection(doc, "SUMMARY");
                addBodyParagraph(doc, user.getAbout().trim());
                addSpacer(doc);
            }

            if (!skills.isEmpty()) {
                addSection(doc, "SKILLS");
                String skillLine = skills.stream()
                        .map(UserSkill::getSkillName)
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining("  •  "));
                addBodyParagraph(doc, skillLine);
                addSpacer(doc);
            }

            if (!experiences.isEmpty()) {
                addSection(doc, "EXPERIENCE");
                for (Experience exp : experiences) {
                    addExperienceEntry(doc, exp);
                }
                addSpacer(doc);
            }

            if (!educations.isEmpty()) {
                addSection(doc, "EDUCATION");
                for (Education edu : educations) {
                    addEducationEntry(doc, edu);
                }
                addSpacer(doc);
            }

            if (!projects.isEmpty()) {
                addSection(doc, "PROJECTS");
                for (Project proj : projects) {
                    addProjectEntry(doc, proj);
                }
            }

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF resume: " + e.getMessage(), e);
        }
    }

    // ── Header ─────────────────────────────────────────────────────────────────

    private void addHeader(Document doc, User user) throws DocumentException {
        // Full name — large, bold, blue
        String fullName = trim(user.getFirstName()) + " " + trim(user.getLastName());
        Font nameFont = new Font(Font.HELVETICA, 24, Font.BOLD, HEADER_COLOR);
        Paragraph name = new Paragraph(fullName.trim(), nameFont);
        name.setAlignment(Element.ALIGN_CENTER);
        doc.add(name);

        // Position and company
        if (notBlank(user.getPosition())) {
            StringBuilder subtitle = new StringBuilder(user.getPosition());
            if (notBlank(user.getCompany())) subtitle.append("  at  ").append(user.getCompany());
            Font subFont = new Font(Font.HELVETICA, 11, Font.NORMAL, SUBTLE_COLOR);
            Paragraph sub = new Paragraph(subtitle.toString(), subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingBefore(2);
            doc.add(sub);
        }

        // Contact line: email | location
        StringBuilder contact = new StringBuilder();
        if (notBlank(user.getEmail()))    contact.append(user.getEmail());
        if (notBlank(user.getLocation())) {
            if (!contact.isEmpty()) contact.append("   |   ");
            contact.append(user.getLocation());
        }
        if (!contact.isEmpty()) {
            Font contactFont = new Font(Font.HELVETICA, 9, Font.NORMAL, SUBTLE_COLOR);
            Paragraph contactP = new Paragraph(contact.toString(), contactFont);
            contactP.setAlignment(Element.ALIGN_CENTER);
            contactP.setSpacingBefore(2);
            doc.add(contactP);
        }

        // Horizontal rule
        addHorizontalRule(doc);
    }

    // ── Section heading ────────────────────────────────────────────────────────

    private void addSection(Document doc, String title) throws DocumentException {
        Font sectionFont = new Font(Font.HELVETICA, 10, Font.BOLD, HEADER_COLOR);
        Paragraph p = new Paragraph(title, sectionFont);
        p.setSpacingBefore(8);
        p.setSpacingAfter(2);
        doc.add(p);
        addHorizontalRule(doc);
    }

    // ── Experience entry ───────────────────────────────────────────────────────

    private void addExperienceEntry(Document doc, Experience exp) throws DocumentException {
        // Title row: job title (bold) + date range (right-aligned)
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{75, 25});
        table.setSpacingBefore(6);

        Font boldFont  = new Font(Font.HELVETICA, 10, Font.BOLD, SECTION_COLOR);
        Font grayFont  = new Font(Font.HELVETICA, 9,  Font.NORMAL, SUBTLE_COLOR);

        PdfPCell titleCell = noBorderCell(new Phrase(exp.getJobTitle() != null ? exp.getJobTitle() : "", boldFont));
        titleCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        String dateRange = formatDate(exp.getStartDate()) + " – " +
                (exp.getEndDate() != null ? formatDate(exp.getEndDate()) : "Present");
        PdfPCell dateCell = noBorderCell(new Phrase(dateRange, grayFont));
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(titleCell);
        table.addCell(dateCell);

        // Company row
        PdfPCell companyCell = noBorderCell(new Phrase(exp.getCompanyName() != null ? exp.getCompanyName() : "", grayFont));
        companyCell.setColspan(2);
        table.addCell(companyCell);

        doc.add(table);

        // Description
        if (notBlank(exp.getDescription())) {
            for (String line : exp.getDescription().trim().split("\n")) {
                if (line.isBlank()) continue;
                Font bodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL, BODY_COLOR);
                Paragraph bullet = new Paragraph("• " + line.trim(), bodyFont);
                bullet.setIndentationLeft(12);
                bullet.setSpacingBefore(2);
                doc.add(bullet);
            }
        }
    }

    // ── Education entry ────────────────────────────────────────────────────────

    private void addEducationEntry(Document doc, Education edu) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{75, 25});
        table.setSpacingBefore(6);

        Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, SECTION_COLOR);
        Font grayFont = new Font(Font.HELVETICA, 9,  Font.NORMAL, SUBTLE_COLOR);

        // Degree + field
        StringBuilder degreeText = new StringBuilder();
        if (notBlank(edu.getDegree())) degreeText.append(edu.getDegree());
        if (notBlank(edu.getFieldOfStudy())) {
            if (!degreeText.isEmpty()) degreeText.append(" in ");
            degreeText.append(edu.getFieldOfStudy());
        }

        PdfPCell degreeCell = noBorderCell(new Phrase(degreeText.toString(), boldFont));
        degreeCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        String dateRange = formatDate(edu.getStartDate()) + " – " +
                (edu.getEndDate() != null ? formatDate(edu.getEndDate()) : "Present");
        PdfPCell dateCell = noBorderCell(new Phrase(dateRange, grayFont));
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(degreeCell);
        table.addCell(dateCell);

        PdfPCell instCell = noBorderCell(new Phrase(edu.getInstitution() != null ? edu.getInstitution() : "", grayFont));
        instCell.setColspan(2);
        table.addCell(instCell);

        doc.add(table);
    }

    // ── Project entry ──────────────────────────────────────────────────────────

    private void addProjectEntry(Document doc, Project proj) throws DocumentException {
        Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD, SECTION_COLOR);
        Font bodyFont = new Font(Font.HELVETICA, 9,  Font.NORMAL, BODY_COLOR);
        Font linkFont = new Font(Font.HELVETICA, 9,  Font.NORMAL, HEADER_COLOR);

        Paragraph title = new Paragraph(proj.getProjectName(), boldFont);
        title.setSpacingBefore(6);
        doc.add(title);

        if (notBlank(proj.getDescription())) {
            Paragraph desc = new Paragraph(proj.getDescription().trim(), bodyFont);
            desc.setIndentationLeft(12);
            desc.setSpacingBefore(2);
            doc.add(desc);
        }

        if (notBlank(proj.getProjectUrl())) {
            Paragraph url = new Paragraph(proj.getProjectUrl(), linkFont);
            url.setIndentationLeft(12);
            url.setSpacingBefore(1);
            doc.add(url);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void addBodyParagraph(Document doc, String text) throws DocumentException {
        Font bodyFont = new Font(Font.HELVETICA, 9, Font.NORMAL, BODY_COLOR);
        Paragraph p = new Paragraph(text, bodyFont);
        p.setSpacingBefore(4);
        p.setLeading(13);
        doc.add(p);
    }

    private void addHorizontalRule(Document doc) throws DocumentException {
        LineSeparator line = new LineSeparator(0.5f, 100, DIVIDER_COLOR, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(line));
        doc.add(Chunk.NEWLINE);
    }

    private void addSpacer(Document doc) throws DocumentException {
        doc.add(new Paragraph(" "));
    }

    private PdfPCell noBorderCell(Phrase content) {
        PdfPCell cell = new PdfPCell(content);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(1);
        return cell;
    }

    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "";
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String trim(String s) {
        return s != null ? s.trim() : "";
    }
}
