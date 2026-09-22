package com.linkedin.backend.features.profile.repository;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.profile.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findByUserOrderByStartDateDesc(User user);
    List<Education> findByUserIdOrderByStartDateDesc(Long userId);
}
