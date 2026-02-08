package com.fintrack.application.user;

import java.util.List;
import java.util.Set;

import com.fintrack.domain.user.Email;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserConnection;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.infrastructure.persistence.user.UserConnectionJpaRepository;
import com.fintrack.infrastructure.security.PasswordService;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for user management operations:
 * registration, Circle of Trust (connected users), and connection logic.
 */
@Service
public class UserService {

    private static final String MSG_USER_NOT_FOUND = "User not found with email: ";
    private static final String MSG_CANNOT_CONNECT_SELF = "You cannot connect to yourself";
    private static final String MSG_ALREADY_CONNECTED = "Users are already connected";

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final UserConnectionJpaRepository userConnectionRepository;

    public UserService(final UserRepository theUserRepository,
                       final PasswordService thePasswordService,
                       final UserConnectionJpaRepository theUserConnectionRepository) {
        Validate.notNull(theUserRepository, "The userRepository cannot be null.");
        Validate.notNull(thePasswordService, "The passwordService cannot be null.");
        Validate.notNull(theUserConnectionRepository, "The userConnectionRepository cannot be null.");

        userRepository = theUserRepository;
        passwordService = thePasswordService;
        userConnectionRepository = theUserConnectionRepository;
    }

    /**
     * Returns all users connected to the given user (Circle of Trust).
     *
     * @param user the user whose connections to load
     * @return list of connected users, never null
     */
    public List<User> getConnectedUsers(User user) {
        return userConnectionRepository.findConnectedUsers(user);
    }

    /**
     * Connects two users bidirectionally (Circle of Trust).
     *
     * @param user the user initiating the connection
     * @param emailToConnect email of the user to connect with
     * @throws IllegalArgumentException if user not found, connecting to self, or already connected
     */
    @Transactional
    public void connectUsers(User user, String emailToConnect) {
        User connectedUser = userRepository.findByEmail(Email.of(emailToConnect))
            .orElseThrow(() -> new IllegalArgumentException(MSG_USER_NOT_FOUND + emailToConnect));

        if (user.equals(connectedUser)) {
            throw new IllegalArgumentException(MSG_CANNOT_CONNECT_SELF);
        }

        if (userConnectionRepository.findByUserAndConnectedUser(user, connectedUser).isPresent()) {
            throw new IllegalArgumentException(MSG_ALREADY_CONNECTED);
        }

        userConnectionRepository.save(new UserConnection(user, connectedUser));
        userConnectionRepository.save(new UserConnection(connectedUser, user));
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
        User user = User.createLocalUser(name, email, encodedPassword, roles);

        return userRepository.save(user);
    }
}