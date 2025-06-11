package com.fintrack.infrastructure.user;

import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository interface for User entity persistence operations.
 * Provides data access methods for user management.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Finds a user by their email address.
     *
     * @param email the email to search for. Cannot be blank.
     *
     * @return an Optional containing the user if found, empty otherwise.
     */
    Optional<User> findByEmail(Email email);

    /**
     * Finds a user by their name.
     *
     * @param name the name to search for. Cannot be blank.
     *
     * @return an Optional containing the user if found, empty otherwise.
     */
    Optional<User> findByName(String name);
}