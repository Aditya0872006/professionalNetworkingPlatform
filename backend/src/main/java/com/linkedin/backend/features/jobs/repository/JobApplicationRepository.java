package com.linkedin.backend.features.jobs.repository;

import com.linkedin.backend.features.jobs.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByJobIdOrderByAppliedAtDesc(Long jobId);

    List<JobApplication> findByApplicantIdOrderByAppliedAtDesc(Long applicantId);

    Optional<JobApplication> findByJobIdAndApplicantId(Long jobId, Long applicantId);

    boolean existsByJobIdAndApplicantId(Long jobId, Long applicantId);

    long countByJobId(Long jobId);
}
