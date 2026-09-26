package com.linkedin.backend.features.authentication.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.linkedin.backend.features.feed.model.Post;
import com.linkedin.backend.features.messaging.model.Conversation;
import com.linkedin.backend.features.networking.model.Connection;
import com.linkedin.backend.features.notifications.model.Notification;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "users")
@Indexed(index = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Email
    @Column(unique = true)
    private String email;
    private Boolean emailVerified = false;
    private String emailVerificationToken = null;
    private LocalDateTime emailVerificationTokenExpiryDate = null;
    @JsonIgnore
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    private String passwordResetToken = null;
    private LocalDateTime passwordResetTokenExpiryDate = null;

    @FullTextField(analyzer = "standard")
    private String firstName = null;
    @FullTextField(analyzer = "standard")
    private String lastName = null;
    @FullTextField(analyzer = "standard")
    private String company = null;
    @FullTextField(analyzer = "standard")
    private String position = null;
    private String location = null;
    private String profilePicture = null;
    private String coverPicture = null;
    private Boolean profileComplete = false;
    private String about = null;

    @JsonIgnore
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> receivedNotifications;

    @JsonIgnore
    @OneToMany(mappedBy = "actor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> actedNotifications;

    @JsonIgnore
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts;

    @JsonIgnore
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Conversation> conversationsAsAuthor;

    @JsonIgnore
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Conversation> conversationsAsRecipient;

    @JsonIgnore
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Connection> initiatedConnections;

    @JsonIgnore
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Connection> receivedConnections;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.linkedin.backend.features.profile.model.UserSkill> skills;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.linkedin.backend.features.profile.model.Education> educations;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.linkedin.backend.features.profile.model.Experience> experiences;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<com.linkedin.backend.features.profile.model.Project> projects;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.role = Role.ROLE_USER;
        this.status = UserStatus.ACTIVE;
    }

    public User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    public User() {
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public LocalDateTime getEmailVerificationTokenExpiryDate() {
        return emailVerificationTokenExpiryDate;
    }

    public void setEmailVerificationTokenExpiryDate(LocalDateTime emailVerificationTokenExpiryDate) {
        this.emailVerificationTokenExpiryDate = emailVerificationTokenExpiryDate;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public LocalDateTime getPasswordResetTokenExpiryDate() {
        return passwordResetTokenExpiryDate;
    }

    public void setPasswordResetTokenExpiryDate(LocalDateTime passwordResetTokenExpiryDate) {
        this.passwordResetTokenExpiryDate = passwordResetTokenExpiryDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateProfileCompletionStatus();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateProfileCompletionStatus();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        updateProfileCompletionStatus();
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
        updateProfileCompletionStatus();
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void updateProfileCompletionStatus() {
        this.profileComplete = (this.firstName != null && this.lastName != null && this.company != null
                && this.position != null && this.location != null);
    }

    public Boolean getProfileComplete() {
        return profileComplete;
    }

    public void setProfileComplete(Boolean profileComplete) {
        this.profileComplete = profileComplete;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public List<Notification> getReceivedNotifications() {
        return receivedNotifications;
    }

    public void setReceivedNotifications(List<Notification> receivedNotifications) {
        this.receivedNotifications = receivedNotifications;
    }

    public List<Notification> getActedNotifications() {
        return actedNotifications;
    }

    public void setActedNotifications(List<Notification> actedNotifications) {
        this.actedNotifications = actedNotifications;
    }

    public List<Conversation> getConversationsAsAuthor() {
        return conversationsAsAuthor;
    }

    public void setConversationsAsAuthor(List<Conversation> conversationsAsAuthor) {
        this.conversationsAsAuthor = conversationsAsAuthor;
    }

    public List<Conversation> getConversationsAsRecipient() {
        return conversationsAsRecipient;
    }

    public void setConversationsAsRecipient(List<Conversation> conversationsAsRecipient) {
        this.conversationsAsRecipient = conversationsAsRecipient;
    }

    public List<Connection> getInitiatedConnections() {
        return initiatedConnections;
    }

    public void setInitiatedConnections(List<Connection> initiatedConnections) {
        this.initiatedConnections = initiatedConnections;
    }

    public List<Connection> getReceivedConnections() {
        return receivedConnections;
    }

    public void setReceivedConnections(List<Connection> receivedConnections) {
        this.receivedConnections = receivedConnections;
    }

    public List<com.linkedin.backend.features.profile.model.UserSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<com.linkedin.backend.features.profile.model.UserSkill> skills) {
        this.skills = skills;
    }

    public String getCoverPicture() {
        return coverPicture;
    }

    public void setCoverPicture(String coverPicture) {
        this.coverPicture = coverPicture;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}