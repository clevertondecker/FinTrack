package com.fintrack.infrastructure.persistence.user;

import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository implementation for User entities.
 * Provides database operations for user persistence.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {

    /**
     * Finds a user by email address.
     *
     * @param emailValue the email address to search for. Cannot be null.
     * @return an Optional containing the user if found, empty otherwise. Never null.
     */
    @Query("SELECT u FROM User u WHERE u.email.email = :emailValue")
    Optional<User> findByEmail(@Param("emailValue") String emailValue);

    /**
     * Finds a user by email using the Email value object.
     *
     * @param email the email value object. Cannot be null.
     * @return an Optional containing the user if found, empty otherwise. Never null.
     */
    @Override
    default Optional<User> findByEmail(Email email) {
        return findByEmail(email.getEmail());
    }

    /**
     * Finds a user by name.
     *
     * @param name the user's name. Cannot be null.
     * @return an Optional containing the user if found, empty otherwise. Never null.
     */
    @Override
    Optional<User> findByName(String name);
}

