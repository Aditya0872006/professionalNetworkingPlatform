package com.linkedin.backend.features.groups.repository;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.groups.model.Group;
import com.linkedin.backend.features.groups.model.GroupMember;
import com.linkedin.backend.features.groups.model.GroupMemberRole;
import com.linkedin.backend.features.groups.model.GroupMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupAndUser(Group group, User user);
    List<GroupMember> findByGroupAndStatus(Group group, GroupMemberStatus status);
    List<GroupMember> findByGroupAndRole(Group group, GroupMemberRole role);
    List<GroupMember> findByUserAndStatus(User user, GroupMemberStatus status);
    boolean existsByGroupAndUserAndStatus(Group group, User user, GroupMemberStatus status);
    boolean existsByGroupAndUserAndRole(Group group, User user, GroupMemberRole role);
    long countByGroupAndStatus(Group group, GroupMemberStatus status);
}
