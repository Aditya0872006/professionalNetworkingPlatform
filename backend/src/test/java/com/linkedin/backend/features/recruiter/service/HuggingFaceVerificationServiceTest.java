package com.linkedin.backend.features.recruiter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.recruiter.model.RecruiterProfile;
import com.linkedin.backend.features.recruiter.model.RecruiterTrustLevel;
import com.linkedin.backend.features.recruiter.model.RecruiterVerificationReport;
import com.linkedin.backend.features.recruiter.repository.RecruiterVerificationReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HuggingFaceVerificationServiceTest {

    @Mock
    private RecruiterVerificationReportRepository reportRepository;

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HuggingFaceVerificationService service;

    private RecruiterProfile sampleProfile;

    @BeforeEach
    void setUp() {
        service = new HuggingFaceVerificationService(reportRepository, restTemplate, objectMapper);
        ReflectionTestUtils.setField(service, "hfToken", "\"hf_test_dummy_token_for_mocking\""); // with quotes
        ReflectionTestUtils.setField(service, "hfApiUrl", "https://router.huggingface.co/hf-inference/models/facebook/bart-large-mnli");
        ReflectionTestUtils.setField(service, "enabled", true);

        User user = new User("recruiter@acmecorp.com", "pass", Role.ROLE_RECRUITER);
        user.setFirstName("Alice");
        user.setLastName("Smith");

        sampleProfile = new RecruiterProfile(
                user,
                "Acme Corporation",
                "jobs@acmecorp.com",
                "https://acmecorp.com",
                "Global leader in road runner technology and engineering.",
                "San Francisco, CA"
        );
        sampleProfile.setId(10L);
    }

    @Test
    void analyzeAndSave_withRouterArrayResponse_successfullyBlendsAndSaves() {
        String hfRouterResponse = """
            [
                {"label": "legitimate company", "score": 0.94},
                {"label": "suspicious profile", "score": 0.04},
                {"label": "fake company", "score": 0.01},
                {"label": "spam registration", "score": 0.01}
            ]
            """;

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(hfRouterResponse));
        when(reportRepository.findByRecruiterProfileId(10L)).thenReturn(Optional.empty());
        when(reportRepository.save(any(RecruiterVerificationReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecruiterVerificationReport report = service.analyzeAndSave(sampleProfile);

        assertNotNull(report);
        assertTrue(report.getTrustScore() >= 80);
        assertEquals(RecruiterTrustLevel.LEGITIMATE, report.getTrustLevel());
        assertTrue(report.getModelUsed().contains("HuggingFace"));
        assertFalse(report.getModelUsed().contains("unavailable"));
        assertTrue(report.getReasoning().contains("AI analysis for Acme Corporation"));

        // Verify Authorization header had quotes stripped
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("https://router.huggingface.co/hf-inference/models/facebook/bart-large-mnli"),
                entityCaptor.capture(),
                eq(String.class)
        );
        String authHeader = entityCaptor.getValue().getHeaders().getFirst("Authorization");
        assertEquals("Bearer hf_test_dummy_token_for_mocking", authHeader);
    }

    @Test
    void analyzeAndSave_whenHuggingFaceThrows_gracefullyFallsBackToRules() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));
        when(reportRepository.findByRecruiterProfileId(10L)).thenReturn(Optional.empty());
        when(reportRepository.save(any(RecruiterVerificationReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecruiterVerificationReport report = service.analyzeAndSave(sampleProfile);

        assertNotNull(report);
        assertTrue(report.getModelUsed().contains("HuggingFace model unavailable"));
        assertTrue(report.getTrustScore() > 0); // Rule engine still scored
    }
}
