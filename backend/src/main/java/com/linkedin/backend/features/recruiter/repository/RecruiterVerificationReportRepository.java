package com.linkedin.backend.features.recruiter.repository;

import com.linkedin.backend.features.recruiter.model.RecruiterVerificationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecruiterVerificationReportRepository extends JpaRepository<RecruiterVerificationReport, Long> {
    Optional<RecruiterVerificationReport> findByRecruiterProfileId(Long recruiterProfileId);
}
