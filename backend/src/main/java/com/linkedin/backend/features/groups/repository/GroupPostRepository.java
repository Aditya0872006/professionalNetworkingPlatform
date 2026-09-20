package com.linkedin.backend.features.groups.repository;

import com.linkedin.backend.features.groups.model.Group;
import com.linkedin.backend.features.groups.model.GroupPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupPostRepository extends JpaRepository<GroupPost, Long> {
    List<GroupPost> findByGroupOrderByCreationDateDesc(Group group);
}
