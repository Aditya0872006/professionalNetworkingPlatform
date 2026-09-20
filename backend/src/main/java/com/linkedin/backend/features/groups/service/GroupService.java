package com.linkedin.backend.features.groups.service;

import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.authentication.repository.UserRepository;
import com.linkedin.backend.features.groups.dto.GroupDetailsDto;
import com.linkedin.backend.features.groups.model.*;
import com.linkedin.backend.features.groups.repository.*;
import com.linkedin.backend.features.notifications.service.NotificationService;
import com.linkedin.backend.features.storage.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPostRepository groupPostRepository;
    private final GroupPostCommentRepository groupPostCommentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final StorageService storageService;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            GroupPostRepository groupPostRepository,
            GroupPostCommentRepository groupPostCommentRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupPostRepository = groupPostRepository;
        this.groupPostCommentRepository = groupPostCommentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.storageService = new StorageService();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private Group getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
    }

    private void requireAdmin(Group group, User user) {
        boolean isAdmin = groupMemberRepository.existsByGroupAndUserAndRole(group, user, GroupMemberRole.ADMIN);
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group admins can perform this action.");
        }
    }

    private boolean isActiveMember(Group group, User user) {
        return groupMemberRepository.existsByGroupAndUserAndStatus(group, user, GroupMemberStatus.ACTIVE);
    }

    private boolean isAdmin(Group group, User user) {
        return groupMemberRepository.existsByGroupAndUserAndRole(group, user, GroupMemberRole.ADMIN)
                && groupMemberRepository.existsByGroupAndUserAndStatus(group, user, GroupMemberStatus.ACTIVE);
    }

    // ─── Group CRUD ─────────────────────────────────────────────────────────────

    public Group createGroup(Long userId, String name, String description, Boolean isPrivate,
                             MultipartFile coverPicture) throws IOException {
        User user = getUser(userId);

        Group group = new Group(name, description, isPrivate != null && isPrivate, user);

        if (coverPicture != null && !coverPicture.isEmpty()) {
            String pictureUrl = storageService.saveImage(coverPicture);
            group.setCoverPicture(pictureUrl);
        }

        Group savedGroup = groupRepository.save(group);

        // Creator automatically becomes ADMIN with ACTIVE status
        GroupMember adminMember = new GroupMember(savedGroup, user, GroupMemberRole.ADMIN, GroupMemberStatus.ACTIVE);
        groupMemberRepository.save(adminMember);

        return savedGroup;
    }

    public List<GroupDetailsDto> searchGroups(String query, Long requestingUserId) {
        List<Group> groups;
        if (query == null || query.isBlank()) {
            groups = groupRepository.findAllByOrderByCreationDateDesc();
        } else {
            groups = groupRepository.findByNameContainingIgnoreCase(query);
        }
        User requestingUser = getUser(requestingUserId);
        return groups.stream().map(group -> {
            long memberCount = groupMemberRepository.countByGroupAndStatus(group, GroupMemberStatus.ACTIVE);
            Optional<GroupMember> memberRecord = groupMemberRepository.findByGroupAndUser(group, requestingUser);
            boolean isMember = memberRecord.isPresent() && memberRecord.get().getStatus() == GroupMemberStatus.ACTIVE;
            boolean isAdmin = memberRecord.isPresent()
                    && memberRecord.get().getStatus() == GroupMemberStatus.ACTIVE
                    && memberRecord.get().getRole() == GroupMemberRole.ADMIN;
            GroupMemberStatus memberStatus = memberRecord.map(GroupMember::getStatus).orElse(null);
            return new GroupDetailsDto(group, memberCount, isMember, isAdmin, memberStatus);
        }).toList();
    }

    public GroupDetailsDto getGroupDetails(Long groupId, Long requestingUserId) {
        Group group = getGroup(groupId);
        User requestingUser = getUser(requestingUserId);

        long memberCount = groupMemberRepository.countByGroupAndStatus(group, GroupMemberStatus.ACTIVE);

        Optional<GroupMember> memberRecord = groupMemberRepository.findByGroupAndUser(group, requestingUser);
        boolean isMember = memberRecord.isPresent() && memberRecord.get().getStatus() == GroupMemberStatus.ACTIVE;
        boolean isAdmin = memberRecord.isPresent()
                && memberRecord.get().getStatus() == GroupMemberStatus.ACTIVE
                && memberRecord.get().getRole() == GroupMemberRole.ADMIN;
        GroupMemberStatus memberStatus = memberRecord.map(GroupMember::getStatus).orElse(null);

        return new GroupDetailsDto(group, memberCount, isMember, isAdmin, memberStatus);
    }

    public List<GroupDetailsDto> getUserGroups(Long userId) {
        User user = getUser(userId);
        List<GroupMember> memberships = groupMemberRepository.findByUserAndStatus(user, GroupMemberStatus.ACTIVE);
        return memberships.stream().map(m -> {
            Group group = m.getGroup();
            long memberCount = groupMemberRepository.countByGroupAndStatus(group, GroupMemberStatus.ACTIVE);
            boolean isAdmin = m.getRole() == GroupMemberRole.ADMIN;
            return new GroupDetailsDto(group, memberCount, true, isAdmin, GroupMemberStatus.ACTIVE);
        }).toList();
    }

    public Group updateGroupVisibility(Long groupId, Long adminId, Boolean isPrivate) {
        Group group = getGroup(groupId);
        User admin = getUser(adminId);
        requireAdmin(group, admin);
        group.setIsPrivate(isPrivate);
        return groupRepository.save(group);
    }

    // ─── Membership ─────────────────────────────────────────────────────────────

    public GroupMember joinGroup(Long groupId, Long userId) {
        Group group = getGroup(groupId);
        User user = getUser(userId);

        // Check if banned
        Optional<GroupMember> existing = groupMemberRepository.findByGroupAndUser(group, user);
        if (existing.isPresent()) {
            GroupMember member = existing.get();
            if (member.getStatus() == GroupMemberStatus.BANNED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are banned from this group.");
            }
            if (member.getStatus() == GroupMemberStatus.ACTIVE) {
                throw new IllegalArgumentException("You are already a member of this group.");
            }
            if (member.getStatus() == GroupMemberStatus.PENDING) {
                throw new IllegalArgumentException("Your join request is already pending.");
            }
        }

        GroupMemberStatus status = group.getIsPrivate() ? GroupMemberStatus.PENDING : GroupMemberStatus.ACTIVE;
        GroupMember member = new GroupMember(group, user, GroupMemberRole.MEMBER, status);
        GroupMember saved = groupMemberRepository.save(member);

        if (group.getIsPrivate()) {
            // Notify all admins of the new join request
            List<GroupMember> admins = groupMemberRepository.findByGroupAndRole(group, GroupMemberRole.ADMIN);
            for (GroupMember admin : admins) {
                notificationService.sendGroupJoinRequestNotification(user, admin.getUser(), groupId);
            }
        }

        return saved;
    }

    public GroupMember acceptJoinRequest(Long groupId, Long adminId, Long targetUserId) {
        Group group = getGroup(groupId);
        User admin = getUser(adminId);
        User target = getUser(targetUserId);
        requireAdmin(group, admin);

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, target)
                .orElseThrow(() -> new IllegalArgumentException("No pending request found for this user."));

        if (member.getStatus() != GroupMemberStatus.PENDING) {
            throw new IllegalArgumentException("User does not have a pending join request.");
        }

        member.setStatus(GroupMemberStatus.ACTIVE);
        GroupMember saved = groupMemberRepository.save(member);
        notificationService.sendGroupJoinAcceptedNotification(admin, target, groupId);
        return saved;
    }

    public void rejectJoinRequest(Long groupId, Long adminId, Long targetUserId) {
        Group group = getGroup(groupId);
        User admin = getUser(adminId);
        User target = getUser(targetUserId);
        requireAdmin(group, admin);

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, target)
                .orElseThrow(() -> new IllegalArgumentException("No pending request found for this user."));

        if (member.getStatus() != GroupMemberStatus.PENDING) {
            throw new IllegalArgumentException("User does not have a pending join request.");
        }

        groupMemberRepository.delete(member);
    }

    public void leaveGroup(Long groupId, Long userId) {
        Group group = getGroup(groupId);
        User user = getUser(userId);

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group."));

        // Prevent sole admin from leaving without transferring ownership
        if (member.getRole() == GroupMemberRole.ADMIN) {
            long adminCount = groupMemberRepository.findByGroupAndRole(group, GroupMemberRole.ADMIN).size();
            if (adminCount == 1) {
                long activeCount = groupMemberRepository.countByGroupAndStatus(group, GroupMemberStatus.ACTIVE);
                if (activeCount > 1) {
                    throw new IllegalArgumentException("You are the sole admin. Promote another member to admin before leaving.");
                }
            }
        }

        groupMemberRepository.delete(member);
    }

    // ─── Admin: Member Management ────────────────────────────────────────────────

    public List<GroupMember> getGroupMembers(Long groupId, Long adminId) {
        Group group = getGroup(groupId);
        User admin = getUser(adminId);
        requireAdmin(group, admin);
        // Return all non-banned members (active + pending)
        List<GroupMember> active = groupMemberRepository.findByGroupAndStatus(group, GroupMemberStatus.ACTIVE);
        List<GroupMember> pending = groupMemberRepository.findByGroupAndStatus(group, GroupMemberStatus.PENDING);
        List<GroupMember> banned = groupMemberRepository.findByGroupAndStatus(group, GroupMemberStatus.BANNED);
        active.addAll(pending);
        active.addAll(banned);
        return active;
    }

    public void removeMember(Long groupId, Long adminId, Long targetUserId) {
        Group group = getGroup(groupId);
        User admin = getUser(adminId);
        User target = getUser(targetUserId);
        requireAdmin(group, admin);

        if (admin.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot remove yourself. Use leave group instead.");
        }

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, target)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this group."));

        groupMemberRepository.delete(member);
    }

    public GroupMember banMember(Long groupId, Long adminId, Long targetUserId) {
        Group group = getGroup(groupId);
        User admin = getUser(adminId);
        User target = getUser(targetUserId);
        requireAdmin(group, admin);

        if (admin.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot ban yourself.");
        }

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, target)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this group."));

        if (member.getRole() == GroupMemberRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot ban another admin.");
        }

        member.setStatus(GroupMemberStatus.BANNED);
        return groupMemberRepository.save(member);
    }

    public GroupMember unbanMember(Long groupId, Long adminId, Long targetUserId) {
        Group group = getGroup(groupId);
        User admin = getUser(adminId);
        User target = getUser(targetUserId);
        requireAdmin(group, admin);

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, target)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this group."));

        if (member.getStatus() != GroupMemberStatus.BANNED) {
            throw new IllegalArgumentException("User is not banned.");
        }

        member.setStatus(GroupMemberStatus.ACTIVE);
        return groupMemberRepository.save(member);
    }

    public GroupMember promoteMember(Long groupId, Long adminId, Long targetUserId) {
        Group group = getGroup(groupId);
        User admin = getUser(adminId);
        User target = getUser(targetUserId);
        requireAdmin(group, admin);

        if (admin.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You are already an admin.");
        }

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, target)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this group."));

        if (member.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active members can be promoted to admin.");
        }

        if (member.getRole() == GroupMemberRole.ADMIN) {
            throw new IllegalArgumentException("User is already an admin.");
        }

        member.setRole(GroupMemberRole.ADMIN);
        return groupMemberRepository.save(member);
    }

    // ─── Group Posts ─────────────────────────────────────────────────────────────

    public List<GroupPost> getGroupPosts(Long groupId, Long requestingUserId) {
        Group group = getGroup(groupId);
        User requestingUser = getUser(requestingUserId);

        // Private group: only members can see posts
        if (group.getIsPrivate() && !isActiveMember(group, requestingUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You must be a member to view posts in a private group.");
        }

        return groupPostRepository.findByGroupOrderByCreationDateDesc(group);
    }

    public GroupPost createGroupPost(Long groupId, Long userId, String content,
                                     MultipartFile picture) throws IOException {
        Group group = getGroup(groupId);
        User user = getUser(userId);

        if (!isActiveMember(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You must be an active member to post in this group.");
        }

        GroupPost post = new GroupPost(content, user, group);
        post.setLikes(new HashSet<>());

        if (picture != null && !picture.isEmpty()) {
            String pictureUrl = storageService.saveImage(picture);
            post.setPicture(pictureUrl);
        }

        GroupPost saved = groupPostRepository.save(post);

        // Notify all active members via WebSocket
        notificationService.sendNewGroupPostNotification(groupId, saved);

        return saved;
    }

    public GroupPost editGroupPost(Long groupId, Long postId, Long userId, String content,
                                   MultipartFile picture) throws IOException {
        Group group = getGroup(groupId);
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));
        User user = getUser(userId);

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own posts.");
        }

        post.setContent(content);

        if (picture != null && !picture.isEmpty()) {
            String pictureUrl = storageService.saveImage(picture);
            post.setPicture(pictureUrl);
        }

        GroupPost saved = groupPostRepository.save(post);
        notificationService.sendEditGroupPostNotification(groupId, postId, saved);
        return saved;
    }

    public void deleteGroupPost(Long groupId, Long postId, Long userId) {
        Group group = getGroup(groupId);
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));
        User user = getUser(userId);

        boolean isAuthor = post.getAuthor().getId().equals(userId);
        boolean isGroupAdmin = isAdmin(group, user);

        if (!isAuthor && !isGroupAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only delete your own posts, or be a group admin.");
        }

        notificationService.sendDeleteGroupPostNotification(groupId, postId);
        groupPostRepository.delete(post);
    }

    public GroupPost likeGroupPost(Long groupId, Long postId, Long userId) {
        getGroup(groupId);
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));
        User user = getUser(userId);

        if (post.getLikes().contains(user)) {
            post.getLikes().remove(user);
        } else {
            post.getLikes().add(user);
        }

        GroupPost saved = groupPostRepository.save(post);
        notificationService.sendGroupPostLikes(groupId, postId, saved.getLikes());
        return saved;
    }

    public Set<User> getGroupPostLikes(Long groupId, Long postId) {
        getGroup(groupId);
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));
        return post.getLikes();
    }

    // ─── Group Post Comments ─────────────────────────────────────────────────────

    public GroupPostComment addComment(Long groupId, Long postId, Long userId, String content) {
        Group group = getGroup(groupId);
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));
        User user = getUser(userId);

        if (!isActiveMember(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You must be a member to comment in this group.");
        }

        GroupPostComment comment = groupPostCommentRepository.save(new GroupPostComment(post, user, content));
        notificationService.sendGroupPostComment(groupId, postId, comment);
        return comment;
    }

    public List<GroupPostComment> getComments(Long groupId, Long postId) {
        getGroup(groupId);
        GroupPost post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));
        return groupPostCommentRepository.findByPostOrderByCreationDateAsc(post);
    }

    public GroupPostComment editComment(Long groupId, Long postId, Long commentId,
                                        Long userId, String content) {
        GroupPostComment comment = groupPostCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found."));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own comments.");
        }

        comment.setContent(content);
        GroupPostComment saved = groupPostCommentRepository.save(comment);
        notificationService.sendGroupPostComment(groupId, postId, saved);
        return saved;
    }

    public void deleteComment(Long groupId, Long postId, Long commentId, Long userId) {
        Group group = getGroup(groupId);
        GroupPostComment comment = groupPostCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found."));
        User user = getUser(userId);

        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        boolean isGroupAdmin = isAdmin(group, user);

        if (!isAuthor && !isGroupAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only delete your own comments, or be a group admin.");
        }

        notificationService.sendDeleteGroupPostComment(groupId, postId, comment);
        groupPostCommentRepository.delete(comment);
    }
}
