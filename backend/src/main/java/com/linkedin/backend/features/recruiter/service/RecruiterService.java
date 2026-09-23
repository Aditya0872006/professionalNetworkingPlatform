package com.linkedin.backend.features.recruiter.service;

import com.linkedin.backend.features.authentication.dto.AuthenticationResponseBody;
import com.linkedin.backend.features.authentication.dto.RecruiterRegistrationDto;
import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.authentication.model.UserStatus;
import com.linkedin.backend.features.authentication.repository.UserRepository;
import com.linkedin.backend.features.authentication.utils.Encoder;
import com.linkedin.backend.features.authentication.utils.JsonWebToken;
import com.linkedin.backend.features.notifications.service.NotificationService;
import com.linkedin.backend.features.recruiter.model.RecruiterProfile;
import com.linkedin.backend.features.recruiter.model.RecruiterStatus;
import com.linkedin.backend.features.recruiter.repository.RecruiterProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RecruiterService {

    private final RecruiterProfileRepository recruiterProfileRepository;
    private final UserRepository userRepository;
    private final Encoder encoder;
    private final JsonWebToken jsonWebToken;
    private final NotificationService notificationService;

    public RecruiterService(RecruiterProfileRepository recruiterProfileRepository,
                            UserRepository userRepository,
                            Encoder encoder,
                            JsonWebToken jsonWebToken,
                            NotificationService notificationService) {
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jsonWebToken = jsonWebToken;
        this.notificationService = notificationService;
    }

    @Transactional
    public AuthenticationResponseBody registerRecruiter(RecruiterRegistrationDto dto) {
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists, please use another email or login.");
        }

        User user = new User(dto.email(), encoder.encode(dto.password()), Role.ROLE_RECRUITER);
        user.setEmailVerified(true);
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPosition(dto.position() != null ? dto.position() : "Recruiter");
        user.setCompany(dto.companyName());
        user.setLocation(dto.location() != null ? dto.location() : dto.companyLocation());
        user.setStatus(UserStatus.ACTIVE);
        user.setProfileComplete(true);
        user.setAbout("Recruiter at " + dto.companyName());

        User savedUser = userRepository.save(user);

        RecruiterProfile profile = new RecruiterProfile(
                savedUser,
                dto.companyName(),
                dto.companyEmail(),
                dto.companyWebsite(),
                dto.companyDescription(),
                dto.companyLocation()
        );
        profile.setStatus(RecruiterStatus.PENDING);
        profile.setRejectionCount(0);
        recruiterProfileRepository.save(profile);

        String token = jsonWebToken.generateToken(savedUser.getEmail());
        return new AuthenticationResponseBody(
                token,
                "Recruiter registered successfully. Your account is pending admin approval.",
                Role.ROLE_RECRUITER.name(),
                RecruiterStatus.PENDING.name(),
                0,
                false
        );
    }

    public Optional<RecruiterProfile> getProfileByUserId(Long userId) {
        return recruiterProfileRepository.findByUserId(userId);
    }

    public List<RecruiterProfile> getAllRecruiterProfiles() {
        return recruiterProfileRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<RecruiterProfile> getRecruiterProfilesByStatus(RecruiterStatus status) {
        return recruiterProfileRepository.findByStatus(status);
    }

    @Transactional
    public RecruiterProfile approveRecruiter(Long profileId) {
        RecruiterProfile profile = recruiterProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Recruiter application not found."));

        if (profile.getStatus() != RecruiterStatus.PENDING) {
            throw new IllegalStateException("Recruiter application is not in PENDING status. Current status: " + profile.getStatus());
        }

        profile.setStatus(RecruiterStatus.APPROVED);
        RecruiterProfile saved = recruiterProfileRepository.save(profile);
        notificationService.sendRecruiterApprovalNotification(profile.getUser());
        return saved;
    }

    @Transactional
    public RecruiterProfile rejectRecruiter(Long profileId) {
        RecruiterProfile profile = recruiterProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Recruiter application not found."));

        if (profile.getStatus() != RecruiterStatus.PENDING) {
            throw new IllegalStateException("Recruiter application is not in PENDING status. Current status: " + profile.getStatus());
        }

        int currentCount = profile.getRejectionCount();
        if (currentCount >= 5) {
            profile.setStatus(RecruiterStatus.PERMANENTLY_REJECTED);
            return recruiterProfileRepository.save(profile);
        }

        int newCount = currentCount + 1;
        profile.setRejectionCount(newCount);

        if (newCount >= 5) {
            profile.setStatus(RecruiterStatus.PERMANENTLY_REJECTED);
            recruiterProfileRepository.save(profile);
            notificationService.sendRecruiterRejectionNotification(profile.getUser(), newCount, true);
        } else {
            profile.setStatus(RecruiterStatus.REJECTED);
            recruiterProfileRepository.save(profile);
            notificationService.sendRecruiterRejectionNotification(profile.getUser(), newCount, false);
        }

        return profile;
    }

    @Transactional
    public RecruiterProfile reapply(Long userId) {
        RecruiterProfile profile = recruiterProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Recruiter application not found."));

        if (profile.getRejectionCount() >= 5 || profile.getStatus() == RecruiterStatus.PERMANENTLY_REJECTED) {
            throw new IllegalStateException("Your recruiter application has reached the maximum number of rejections (5). You cannot reapply.");
        }

        if (profile.getStatus() != RecruiterStatus.REJECTED) {
            throw new IllegalStateException("Only rejected applications can reapply. Current status is " + profile.getStatus());
        }

        profile.setStatus(RecruiterStatus.PENDING);
        return recruiterProfileRepository.save(profile);
    }

    public boolean isApprovedRecruiter(Long userId) {
        return recruiterProfileRepository.findByUserId(userId)
                .map(profile -> profile.getStatus() == RecruiterStatus.APPROVED)
                .orElse(false);
    }
}
