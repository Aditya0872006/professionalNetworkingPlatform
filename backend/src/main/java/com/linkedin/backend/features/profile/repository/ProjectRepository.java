package com.linkedin.backend.features.profile.repository;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.profile.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserOrderByIdDesc(User user);
    List<Project> findByUserIdOrderByIdDesc(Long userId);
}
