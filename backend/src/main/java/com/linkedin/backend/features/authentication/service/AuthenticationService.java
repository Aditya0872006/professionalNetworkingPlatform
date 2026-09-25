package com.linkedin.backend.features.authentication.service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.linkedin.backend.features.authentication.dto.AuthenticationRequestBody;
import com.linkedin.backend.features.authentication.dto.AuthenticationResponseBody;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.authentication.repository.UserRepository;
import com.linkedin.backend.features.authentication.utils.EmailService;
import com.linkedin.backend.features.authentication.utils.Encoder;
import com.linkedin.backend.features.authentication.utils.JsonWebToken;
import com.linkedin.backend.features.storage.service.StorageService;

import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final UserRepository userRepository;
    private final int durationInMinutes = 1;

    private final Encoder encoder;
    private final JsonWebToken jsonWebToken;
    private final EmailService emailService;
    private final RestTemplate restTemplate;
    private final StorageService storageService;
    private final com.linkedin.backend.features.recruiter.repository.RecruiterProfileRepository recruiterProfileRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${oauth.google.client.id}")
    private String googleClientId;

    @Value("${oauth.google.client.secret}")
    private String googleClientSecret;

    public AuthenticationService(UserRepository userRepository, Encoder encoder, JsonWebToken jsonWebToken,
            EmailService emailService, RestTemplate restTemplate,
            com.linkedin.backend.features.recruiter.repository.RecruiterProfileRepository recruiterProfileRepository) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jsonWebToken = jsonWebToken;
        this.emailService = emailService;
        this.restTemplate = restTemplate;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.storageService = new StorageService();
    }

    public static String generateEmailVerificationToken() {
        SecureRandom random = new SecureRandom();
        StringBuilder token = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            token.append(random.nextInt(10));
        }
        return token.toString();
    }

    public void sendEmailVerificationToken(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && !user.get().getEmailVerified()) {
            String emailVerificationToken = generateEmailVerificationToken();
            String hashedToken = encoder.encode(emailVerificationToken);
            user.get().setEmailVerificationToken(hashedToken);
            user.get().setEmailVerificationTokenExpiryDate(LocalDateTime.now().plusMinutes(durationInMinutes));
            userRepository.save(user.get());
            String subject = "Email Verification";
            String body = String.format("Only one step to take full advantage of Our Professional Networking Platform.\n\n"
                    + "Enter this code to verify your email: " + "%s\n\n" + "The code will expire in " + "%s"
                    + " minutes.",
                    emailVerificationToken, durationInMinutes);
            try {
                emailService.sendEmail(email, subject, body);
            } catch (Exception e) {
                logger.info("Error while sending email: {}", e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Email verification token failed, or email is already verified.");
        }
    }

    public void validateEmailVerificationToken(String token, String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && encoder.matches(token, user.get().getEmailVerificationToken())
                && !user.get().getEmailVerificationTokenExpiryDate().isBefore(LocalDateTime.now())) {
            user.get().setEmailVerified(true);
            user.get().setEmailVerificationToken(null);
            user.get().setEmailVerificationTokenExpiryDate(null);
            userRepository.save(user.get());
        } else if (user.isPresent() && encoder.matches(token, user.get().getEmailVerificationToken())
                && user.get().getEmailVerificationTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Email verification token expired.");
        } else {
            throw new IllegalArgumentException("Email verification token failed.");
        }
    }

    private static final java.util.regex.Pattern STRONG_PASSWORD_PATTERN =
            java.util.regex.Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).{8,}$");

    public void validateStrongPassword(String password) {
        if (password == null || !STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one numeric digit, and one special character.");
        }
    }

    public AuthenticationResponseBody login(AuthenticationRequestBody loginRequestBody) {
        User user = userRepository.findByEmail(loginRequestBody.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (!encoder.matches(loginRequestBody.password(), user.getPassword())) {
            throw new IllegalArgumentException("Password is incorrect.");
        }

        if (user.getStatus() == com.linkedin.backend.features.authentication.model.UserStatus.BLOCKED) {
            throw new IllegalArgumentException("Account is blocked. Please contact support.");
        }

        if (loginRequestBody.role() != null && !loginRequestBody.role().isBlank()) {
            // Normalize: strip any existing ROLE_ prefix, then always prepend it once.
            // This prevents "ROLE_USER" sent from the frontend from becoming "ROLE_ROLE_USER".
            String raw = loginRequestBody.role().trim().toUpperCase();
            String normalized = raw.startsWith("ROLE_") ? raw : "ROLE_" + raw;
            if (!user.getRole().name().equals(normalized)) {
                throw new IllegalArgumentException("Access denied: You cannot log in as " +
                        loginRequestBody.role() + " with this account.");
            }
        }

        String token = jsonWebToken.generateToken(loginRequestBody.email());

        if (user.getRole() == com.linkedin.backend.features.authentication.model.Role.ROLE_RECRUITER) {
            Optional<com.linkedin.backend.features.recruiter.model.RecruiterProfile> profileOpt =
                    recruiterProfileRepository.findByUserId(user.getId());
            if (profileOpt.isPresent()) {
                com.linkedin.backend.features.recruiter.model.RecruiterProfile profile = profileOpt.get();
                boolean canReapply = profile.getStatus() == com.linkedin.backend.features.recruiter.model.RecruiterStatus.REJECTED
                        && profile.getRejectionCount() < 5;
                String statusMsg = switch (profile.getStatus()) {
                    case PENDING -> "Your recruiter approval is pending.";
                    case APPROVED -> "Authentication succeeded.";
                    case REJECTED -> "Your recruiter application was rejected (Rejections: " + profile.getRejectionCount() + "/5).";
                    case PERMANENTLY_REJECTED -> "Your recruiter application has reached the maximum number of rejections (5). You cannot reapply.";
                };
                return new AuthenticationResponseBody(
                        token,
                        statusMsg,
                        user.getRole().name(),
                        profile.getStatus().name(),
                        profile.getRejectionCount(),
                        canReapply
                );
            }
        }

        return new AuthenticationResponseBody(
                token,
                "Authentication succeeded.",
                user.getRole().name(),
                user.getStatus().name(),
                null,
                null
        );
    }

    public AuthenticationResponseBody googleLoginOrSignup(String code, String page) {
        String tokenEndpoint = "https://oauth2.googleapis.com/token";
        String redirectUri = "http://localhost:5173/authentication/" + page;
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(tokenEndpoint, HttpMethod.POST, request,
                new ParameterizedTypeReference<>() {
                });

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            String idToken = (String) responseBody.get("id_token");

            Claims claims = jsonWebToken.getClaimsFromGoogleOauthIdToken(idToken);
            String email = claims.get("email", String.class);
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                Boolean emailVerified = claims.get("email_verified", Boolean.class);
                String firstName = claims.get("given_name", String.class);
                String lastName = claims.get("family_name", String.class);
                User newUser = new User(email, null);
                newUser.setEmailVerified(emailVerified);
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                userRepository.save(newUser);
            }

            String token = jsonWebToken.generateToken(email);
            return new AuthenticationResponseBody(token, "Google authentication succeeded.");
        } else {
            throw new IllegalArgumentException("Failed to exchange code for ID token.");
        }
    }

    public AuthenticationResponseBody register(AuthenticationRequestBody registerRequestBody) {
        if (userRepository.existsByEmail(registerRequestBody.email())) {
            throw new IllegalArgumentException(
                    "An account with this email already exists.");
        }
        validateStrongPassword(registerRequestBody.password());

        User user = userRepository.save(new User(
                registerRequestBody.email(),
                encoder.encode(registerRequestBody.password()),
                com.linkedin.backend.features.authentication.model.Role.ROLE_USER));

        String emailVerificationToken = generateEmailVerificationToken();
        String hashedToken = encoder.encode(emailVerificationToken);
        user.setEmailVerificationToken(hashedToken);
        user.setEmailVerificationTokenExpiryDate(LocalDateTime.now().plusMinutes(durationInMinutes));

        userRepository.save(user);

        String subject = "Email Verification";
        String body = String.format("""
                Only one step to take full advantage of Professional Networking Platform.

                Enter this code to verify your email: %s. The code will expire in %s minutes.""",
                emailVerificationToken, durationInMinutes);
        try {
            emailService.sendEmail(registerRequestBody.email(), subject, body);
        } catch (Exception e) {
            logger.info("Error while sending email: {}", e.getMessage());
        }
        String authToken = jsonWebToken.generateToken(registerRequestBody.email());
        return new AuthenticationResponseBody(authToken, "User registered successfully.");
    }

    public User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = entityManager.find(User.class, userId);
        if (user != null) {
            entityManager.createNativeQuery("DELETE FROM posts_likes WHERE user_id = :userId")
                    .setParameter("userId", userId)
                    .executeUpdate();
            entityManager.remove(user);
        }
    }

    public void sendPasswordResetToken(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            String passwordResetToken = generateEmailVerificationToken();
            String hashedToken = encoder.encode(passwordResetToken);
            user.get().setPasswordResetToken(hashedToken);
            user.get().setPasswordResetTokenExpiryDate(LocalDateTime.now().plusMinutes(durationInMinutes));
            userRepository.save(user.get());
            String subject = "Password Reset";
            String body = String.format("""
                    You requested a password reset.

                    Enter this code to reset your password: %s. The code will expire in %s minutes.""",
                    passwordResetToken, durationInMinutes);
            try {
                emailService.sendEmail(email, subject, body);
            } catch (Exception e) {
                logger.info("Error while sending email: {}", e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("User not found.");
        }
    }

    public void resetPassword(String email, String newPassword, String token) {
        validateStrongPassword(newPassword);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && encoder.matches(token, user.get().getPasswordResetToken())
                && !user.get().getPasswordResetTokenExpiryDate().isBefore(LocalDateTime.now())) {
            user.get().setPasswordResetToken(null);
            user.get().setPasswordResetTokenExpiryDate(null);
            user.get().setPassword(encoder.encode(newPassword));
            userRepository.save(user.get());
        } else if (user.isPresent() && encoder.matches(token, user.get().getPasswordResetToken())
                && user.get().getPasswordResetTokenExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token expired.");
        } else {
            throw new IllegalArgumentException("Password reset token failed.");
        }
    }

    public User updateUserProfile(User user, String firstName, String lastName, String company,
            String position, String location, String about) {
        if (firstName != null)
            user.setFirstName(firstName);
        if (lastName != null)
            user.setLastName(lastName);
        if (company != null)
            user.setCompany(company);
        if (position != null)
            user.setPosition(position);
        if (location != null)
            user.setLocation(location);
        if (about != null)
            user.setAbout(about);

        return userRepository.save(user);
    }

    public User updateProfilePicture(User user, MultipartFile profilePicture) throws IOException {
        if (profilePicture != null) {
            String profilePictureUrl = storageService.saveImage(profilePicture);
            user.setProfilePicture(profilePictureUrl);
        } else {
            if (user.getProfilePicture() != null)
                storageService.deleteFile(user.getProfilePicture());

            user.setProfilePicture(null);
        }
        return userRepository.save(user);
    }

    public User updateCoverPicture(User user, MultipartFile coverPicture) throws IOException {
        if (coverPicture != null) {
            String coverPictureUrl = storageService.saveImage(coverPicture);
            user.setCoverPicture(coverPictureUrl);
        } else {
            if (user.getCoverPicture() != null)
                storageService.deleteFile(user.getCoverPicture());

            user.setCoverPicture(null);
        }

        return userRepository.save(user);
    }

    public User getUserById(Long receiverId) {
        return userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

}