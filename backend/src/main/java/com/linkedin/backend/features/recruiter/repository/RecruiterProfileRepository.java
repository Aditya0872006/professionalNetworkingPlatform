package com.linkedin.backend.features.recruiter.repository;

import com.linkedin.backend.features.recruiter.model.RecruiterProfile;
import com.linkedin.backend.features.recruiter.model.RecruiterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {
    Optional<RecruiterProfile> findByUserId(Long userId);

    List<RecruiterProfile> findByStatus(RecruiterStatus status);

    List<RecruiterProfile> findAllByOrderByCreatedAtDesc();

    long countByStatus(RecruiterStatus status);
}
