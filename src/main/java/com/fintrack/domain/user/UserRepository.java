package com.fintrack.domain.user;

import java.util.Optional;

/**
 * Repository interface for User entity persistence operations.
 * Provides data access methods for user management.
 */
public interface UserRepository {
    Optional<User> findByEmail(Email email);
    Optional<User> findByName(String name);
    User save(User user);
    Optional<User> findById(Long id);
    void deleteById(Long id);
} 