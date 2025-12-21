package com.fintrack.controller.user;

import java.util.List;
import java.util.Set;

import com.fintrack.domain.user.UserRepository;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Email;
import com.fintrack.dto.user.RegisterRequest;
import com.fintrack.dto.user.RegisterResponse;
import com.fintrack.dto.user.CurrentUserResponse;
import com.fintrack.dto.user.UserListResponse;
import com.fintrack.dto.user.UserResponse;

import jakarta.validation.Valid;
import java.util.Optional;

/**
 * REST controller for user-related operations, such as registration.
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(final UserService theUserService, final UserRepository theUserRepository) {
        Validate.notNull(theUserService, "the userService cannot be null.");
        Validate.notNull(theUserRepository, "the userRepository cannot be null.");
        userService = theUserService;
        userRepository = theUserRepository;
    }

    /**
     * Registers a new user with the default USER role.
     *
     * @param request  the registration details containing name, email, and password
     *
     * @return a response entity with a success message if registration succeeds
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Set<Role> roles = Set.of(Role.USER);
            userService.registerUser(
              request.name(), request.email(), request.password(), roles);

            return ResponseEntity.ok(
              new RegisterResponse("User registered successfully"));

        } catch (Exception e) {
            logger.error("Error during registration: ", e);
            throw e;
        }
    }

    /**
     * Gets the current authenticated user's information.
     *
     * @param userDetails the authenticated user details
     * @return a response with the user's information
     */
    @GetMapping("/current-user")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(Email.of(userDetails.getUsername()));
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            User user = userOpt.get();
            CurrentUserResponse response = new CurrentUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail().getEmail(),
                user.getRoles().stream().map(Role::name).toArray(String[]::new),
                user.getCreatedAt(),
                user.getUpdatedAt()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting current user: ", e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    /**
     * Gets all users in the system.
     *
     * @return a response with all users' information
     */
    @GetMapping
    public ResponseEntity<UserListResponse> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            List<UserResponse> userResponses = users.stream()
                .map(user -> new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail().getEmail(),
                    user.getRoles().stream().map(Role::name).toArray(String[]::new),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
                ))
                .toList();

            UserListResponse response = new UserListResponse(
                "Users retrieved successfully",
                userResponses, userResponses.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting all users: ", e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
}
