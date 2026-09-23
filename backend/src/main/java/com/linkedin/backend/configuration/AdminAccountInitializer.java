package com.linkedin.backend.configuration;

import com.linkedin.backend.features.authentication.model.Role;
import com.linkedin.backend.features.authentication.model.User;
import com.linkedin.backend.features.authentication.model.UserStatus;
import com.linkedin.backend.features.authentication.repository.UserRepository;
import com.linkedin.backend.features.authentication.utils.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final Encoder encoder;

    @Value("${admin1.email:admin1@gmail.com}")
    private String admin1Email;

    @Value("${admin1.password:admin@First}")
    private String admin1Password;

    @Value("${admin2.email:admin2@gmail.com}")
    private String admin2Email;

    @Value("${admin2.password:admin@Second}")
    private String admin2Password;

    public AdminAccountInitializer(UserRepository userRepository, Encoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        seedAdminUser(admin1Email, admin1Password, "Admin", "One");
        seedAdminUser(admin2Email, admin2Password, "Admin", "Two");
    }

    private void seedAdminUser(String email, String rawPassword, String firstName, String lastName) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User admin = new User();
            admin.setEmail(email);
            admin.setPassword(encoder.encode(rawPassword));
            admin.setRole(Role.ROLE_ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setEmailVerified(true);
            admin.setFirstName(firstName);
            admin.setLastName(lastName);
            admin.setPosition("Platform Administrator");
            admin.setCompany("LinkedIn Clone");
            admin.setLocation("Global");
            admin.setProfileComplete(true);
            admin.setAbout("Platform administrator account.");
            userRepository.save(admin);
            logger.info("Admin account seeded successfully: {}", email);
        }
    }
}
