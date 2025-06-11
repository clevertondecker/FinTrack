package com.fintrack.application.user;

import java.util.Set;

import com.fintrack.infrastructure.security.PasswordService;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.user.UserRepository;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for user management operations.
 * Handles user registration and other user-related business logic.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public UserService(final UserRepository theUserRepository,
                       final PasswordService thePasswordService) {
        Validate.notNull(theUserRepository, "The userRepository cannot be null.");
        Validate.notNull(thePasswordService, "The passwordService cannot be null.");

        userRepository = theUserRepository;
        passwordService = thePasswordService ;
    }

    /**
     * Registers a new user in the system.
     *
     * @param name the user's name. Cannot be blank.
     *
     * @param email the user's email. Cannot be blank.
     *
     * @param rawPassword the raw password to be encoded. Cannot be blank.
     *
     * @param roles the user's roles. Cannot be null or empty.
     *
     * @return the created user entity. Never null.
     */
    @Transactional
    public User registerUser(final String name, final String email,
                             final String rawPassword, final Set<Role> roles) {
        String encodedPassword = passwordService.encodePassword(rawPassword);
        User user = User.of(name, email, encodedPassword, roles);

        return userRepository.save(user);
    }
}