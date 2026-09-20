package com.linkedin.backend.features.groups.repository;

import com.linkedin.backend.features.groups.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByNameContainingIgnoreCase(String name);
    List<Group> findAllByOrderByCreationDateDesc();
}
