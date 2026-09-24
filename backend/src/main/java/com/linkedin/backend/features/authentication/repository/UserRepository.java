    package com.linkedin.backend.features.authentication.repository;

    import java.util.List;
    import java.util.Optional;

    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.stereotype.Repository;

    import com.linkedin.backend.features.authentication.model.Role;
    import com.linkedin.backend.features.authentication.model.User;
    import com.linkedin.backend.features.authentication.model.UserStatus;

    @Repository
    public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByEmail(String email);
        boolean existsByEmail(String email);

        List<User> findAllByIdNot(Long id);

        long countByRole(Role role);

        long countByStatus(UserStatus status);

        List<User> findByRole(Role role);

        List<User> findByStatus(UserStatus status);

        List<User> findByRoleAndStatus(Role role, UserStatus status);
    }
