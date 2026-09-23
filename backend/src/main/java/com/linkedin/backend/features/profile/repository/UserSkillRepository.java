package com.linkedin.backend.features.profile.repository;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.profile.model.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUserOrderByIdAsc(User user);
    List<UserSkill> findByUserIdOrderByIdAsc(Long userId);
    List<UserSkill> findBySkillNameIgnoreCaseIn(List<String> skillNames);
}
