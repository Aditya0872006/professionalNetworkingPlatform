package com.linkedin.backend.features.groups.repository;

import com.linkedin.backend.features.groups.model.GroupPost;
import com.linkedin.backend.features.groups.model.GroupPostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupPostCommentRepository extends JpaRepository<GroupPostComment, Long> {
    List<GroupPostComment> findByPostOrderByCreationDateAsc(GroupPost post);
}
