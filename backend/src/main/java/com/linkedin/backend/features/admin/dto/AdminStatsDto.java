package com.linkedin.backend.features.admin.dto;

public record AdminStatsDto(
        long totalUsers,
        long totalRecruiters,
        long pendingRecruiterRequests,
        long totalJobs,
        long totalPosts,
        long blockedUsers
) {
}
