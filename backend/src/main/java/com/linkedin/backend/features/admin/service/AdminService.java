package com.linkedin.backend.features.admin.service;

import com.linkedin.backend.features.admin.dto.AdminStatsDto;
import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.authentication.model.UserStatus;
import com.linkedin.backend.features.authentication.repository.UserRepository;
import com.linkedin.backend.features.feed.model.Post;
import com.linkedin.backend.features.feed.repository.PostRepository;
import com.linkedin.backend.features.jobs.model.Job;
import com.linkedin.backend.features.jobs.repository.JobRepository;
import com.linkedin.backend.features.jobs.service.JobService;
import com.linkedin.backend.features.notifications.service.NotificationService;
import com.linkedin.backend.features.recruiter.model.RecruiterProfile;
import com.linkedin.backend.features.recruiter.model.RecruiterStatus;
import com.linkedin.backend.features.recruiter.repository.RecruiterProfileRepository;
import com.linkedin.backend.features.recruiter.service.RecruiterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final RecruiterService recruiterService;
    private final JobRepository jobRepository;
    private final JobService jobService;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    public AdminService(UserRepository userRepository,
                        RecruiterProfileRepository recruiterProfileRepository,
                        RecruiterService recruiterService,
                        JobRepository jobRepository,
                        JobService jobService,
                        PostRepository postRepository,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.recruiterService = recruiterService;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.postRepository = postRepository;
        this.notificationService = notificationService;
    }

    public AdminStatsDto getStats() {
        long totalUsers = userRepository.countByRole(Role.ROLE_USER);
        long totalRecruiters = userRepository.countByRole(Role.ROLE_RECRUITER);
        long pendingRequests = recruiterProfileRepository.countByStatus(RecruiterStatus.PENDING);
        long totalJobs = jobRepository.count();
        long totalPosts = postRepository.count();
        long blockedUsers = userRepository.countByStatus(UserStatus.BLOCKED);

        return new AdminStatsDto(
                totalUsers,
                totalRecruiters,
                pendingRequests,
                totalJobs,
                totalPosts,
                blockedUsers
        );
    }

    public List<User> getAllUsers(Role role, UserStatus status) {
        if (role != null && status != null) {
            return userRepository.findByRoleAndStatus(role, status);
        } else if (role != null) {
            return userRepository.findByRole(role);
        } else if (status != null) {
            return userRepository.findByStatus(status);
        }
        return userRepository.findAll();
    }

    @Transactional
    public User blockUser(Long userId, User admin) {
        validateAdmin(admin);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (target.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot block an administrator.");
        }

        target.setStatus(UserStatus.BLOCKED);
        User saved = userRepository.save(target);
        notificationService.sendUserBlockedNotification(target);
        return saved;
    }

    @Transactional
    public User unblockUser(Long userId, User admin) {
        validateAdmin(admin);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        target.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(target);
        notificationService.sendUserUnblockedNotification(target);
        return saved;
    }

    public List<RecruiterProfile> getRecruiterApplications(RecruiterStatus status) {
        if (status != null) {
            return recruiterService.getRecruiterProfilesByStatus(status);
        }
        return recruiterService.getAllRecruiterProfiles();
    }

    @Transactional
    public RecruiterProfile approveRecruiter(Long profileId, User admin) {
        validateAdmin(admin);
        return recruiterService.approveRecruiter(profileId);
    }

    @Transactional
    public RecruiterProfile rejectRecruiter(Long profileId, User admin) {
        validateAdmin(admin);
        return recruiterService.rejectRecruiter(profileId);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreationDateDesc();
    }

    @Transactional
    public void deletePost(Long postId, User admin) {
        validateAdmin(admin);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));

        User author = post.getAuthor();
        postRepository.delete(post);
        notificationService.sendDeleteNotificationToPost(postId);
        if (author != null) {
            notificationService.sendPostRemovedByAdminNotification(author, postId);
        }
    }

    public List<Job> getAllJobs() {
        return jobService.getAllJobsForAdmin();
    }

    @Transactional
    public Job removeJob(Long jobId, String reason, User admin) {
        validateAdmin(admin);
        return jobService.removeJobByAdmin(jobId, reason, admin);
    }

    private void validateAdmin(User user) {
        if (user == null || user.getRole() != Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Access denied: Administrator privileges required.");
        }
    }
}
