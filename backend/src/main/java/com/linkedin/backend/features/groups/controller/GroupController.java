package com.linkedin.backend.features.groups.controller;

import com.linkedin.backend.dto.Response;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.groups.dto.GroupDetailsDto;
import com.linkedin.backend.features.groups.dto.GroupPostCommentDto;
import com.linkedin.backend.features.groups.model.Group;
import com.linkedin.backend.features.groups.model.GroupMember;
import com.linkedin.backend.features.groups.model.GroupPost;
import com.linkedin.backend.features.groups.model.GroupPostComment;
import com.linkedin.backend.features.groups.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // ─── Group discovery & creation ───────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<GroupDetailsDto>> searchGroups(
            @RequestParam(required = false) String query,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.searchGroups(query, user.getId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<GroupDetailsDto>> getMyGroups(
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.getUserGroups(user.getId()));
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isPrivate", required = false) Boolean isPrivate,
            @RequestParam(value = "coverPicture", required = false) MultipartFile coverPicture,
            @RequestAttribute("authenticatedUser") User user) throws IOException {
        return ResponseEntity.ok(groupService.createGroup(user.getId(), name, description, isPrivate, coverPicture));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailsDto> getGroupDetails(
            @PathVariable Long groupId,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.getGroupDetails(groupId, user.getId()));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<Group> updateGroupVisibility(
            @PathVariable Long groupId,
            @RequestParam Boolean isPrivate,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.updateGroupVisibility(groupId, user.getId(), isPrivate));
    }

    // ─── Membership ───────────────────────────────────────────────────────────

    @PostMapping("/{groupId}/join")
    public ResponseEntity<GroupMember> joinGroup(
            @PathVariable Long groupId,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.joinGroup(groupId, user.getId()));
    }

    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<Response> leaveGroup(
            @PathVariable Long groupId,
            @RequestAttribute("authenticatedUser") User user) {
        groupService.leaveGroup(groupId, user.getId());
        return ResponseEntity.ok(new Response("Left group successfully."));
    }

    // ─── Admin: member management ─────────────────────────────────────────────

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMember>> getGroupMembers(
            @PathVariable Long groupId,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.getGroupMembers(groupId, user.getId()));
    }

    @PutMapping("/{groupId}/members/{targetUserId}/accept")
    public ResponseEntity<GroupMember> acceptJoinRequest(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.acceptJoinRequest(groupId, user.getId(), targetUserId));
    }

    @DeleteMapping("/{groupId}/members/{targetUserId}/reject")
    public ResponseEntity<Response> rejectJoinRequest(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @RequestAttribute("authenticatedUser") User user) {
        groupService.rejectJoinRequest(groupId, user.getId(), targetUserId);
        return ResponseEntity.ok(new Response("Join request rejected."));
    }

    @DeleteMapping("/{groupId}/members/{targetUserId}")
    public ResponseEntity<Response> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @RequestAttribute("authenticatedUser") User user) {
        groupService.removeMember(groupId, user.getId(), targetUserId);
        return ResponseEntity.ok(new Response("Member removed successfully."));
    }

    @PutMapping("/{groupId}/members/{targetUserId}/ban")
    public ResponseEntity<GroupMember> banMember(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.banMember(groupId, user.getId(), targetUserId));
    }

    @PutMapping("/{groupId}/members/{targetUserId}/unban")
    public ResponseEntity<GroupMember> unbanMember(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.unbanMember(groupId, user.getId(), targetUserId));
    }

    @PutMapping("/{groupId}/members/{targetUserId}/promote")
    public ResponseEntity<GroupMember> promoteMember(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.promoteMember(groupId, user.getId(), targetUserId));
    }

    // ─── Group Posts ──────────────────────────────────────────────────────────

    @GetMapping("/{groupId}/posts")
    public ResponseEntity<List<GroupPost>> getGroupPosts(
            @PathVariable Long groupId,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.getGroupPosts(groupId, user.getId()));
    }

    @PostMapping("/{groupId}/posts")
    public ResponseEntity<GroupPost> createGroupPost(
            @PathVariable Long groupId,
            @RequestParam("content") String content,
            @RequestParam(value = "picture", required = false) MultipartFile picture,
            @RequestAttribute("authenticatedUser") User user) throws IOException {
        return ResponseEntity.ok(groupService.createGroupPost(groupId, user.getId(), content, picture));
    }

    @PutMapping("/{groupId}/posts/{postId}")
    public ResponseEntity<GroupPost> editGroupPost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @RequestParam("content") String content,
            @RequestParam(value = "picture", required = false) MultipartFile picture,
            @RequestAttribute("authenticatedUser") User user) throws IOException {
        return ResponseEntity.ok(groupService.editGroupPost(groupId, postId, user.getId(), content, picture));
    }

    @DeleteMapping("/{groupId}/posts/{postId}")
    public ResponseEntity<Response> deleteGroupPost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @RequestAttribute("authenticatedUser") User user) {
        groupService.deleteGroupPost(groupId, postId, user.getId());
        return ResponseEntity.ok(new Response("Post deleted successfully."));
    }

    @PutMapping("/{groupId}/posts/{postId}/like")
    public ResponseEntity<GroupPost> likeGroupPost(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.likeGroupPost(groupId, postId, user.getId()));
    }

    @GetMapping("/{groupId}/posts/{postId}/likes")
    public ResponseEntity<Set<User>> getGroupPostLikes(
            @PathVariable Long groupId,
            @PathVariable Long postId) {
        return ResponseEntity.ok(groupService.getGroupPostLikes(groupId, postId));
    }

    // ─── Group Post Comments ──────────────────────────────────────────────────

    @GetMapping("/{groupId}/posts/{postId}/comments")
    public ResponseEntity<List<GroupPostComment>> getComments(
            @PathVariable Long groupId,
            @PathVariable Long postId) {
        return ResponseEntity.ok(groupService.getComments(groupId, postId));
    }

    @PostMapping("/{groupId}/posts/{postId}/comments")
    public ResponseEntity<GroupPostComment> addComment(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @RequestBody GroupPostCommentDto dto,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.addComment(groupId, postId, user.getId(), dto.getContent()));
    }

    @PutMapping("/{groupId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<GroupPostComment> editComment(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody GroupPostCommentDto dto,
            @RequestAttribute("authenticatedUser") User user) {
        return ResponseEntity.ok(groupService.editComment(groupId, postId, commentId, user.getId(), dto.getContent()));
    }

    @DeleteMapping("/{groupId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Response> deleteComment(
            @PathVariable Long groupId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestAttribute("authenticatedUser") User user) {
        groupService.deleteComment(groupId, postId, commentId, user.getId());
        return ResponseEntity.ok(new Response("Comment deleted successfully."));
    }
}
