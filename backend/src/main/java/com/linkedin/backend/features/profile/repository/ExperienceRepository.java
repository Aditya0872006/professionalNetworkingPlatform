package com.linkedin.backend.features.profile.repository;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.profile.model.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    List<Experience> findByUserOrderByStartDateDesc(User user);
    List<Experience> findByUserIdOrderByStartDateDesc(Long userId);
}
