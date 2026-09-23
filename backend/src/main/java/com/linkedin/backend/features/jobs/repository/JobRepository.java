package com.linkedin.backend.features.jobs.repository;

import com.linkedin.backend.features.jobs.model.Job;
import com.linkedin.backend.features.jobs.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);

    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    List<Job> findAllByOrderByCreatedAtDesc();

    long countByStatus(JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.status = 'PUBLISHED' " +
            "AND (:query IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(j.company) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(j.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
            "ORDER BY j.createdAt DESC")
    List<Job> searchPublishedJobs(@Param("query") String query, @Param("location") String location);
}
