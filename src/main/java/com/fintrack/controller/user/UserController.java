package com.fintrack.controller.user;

import java.util.List;
import java.util.Set;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.user.ConnectUserRequest;
import com.fintrack.dto.user.RegisterRequest;
import com.fintrack.dto.user.RegisterResponse;
import com.fintrack.dto.user.CurrentUserResponse;
import com.fintrack.dto.user.UserListResponse;
import com.fintrack.dto.user.UserResponse;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Optional;

/**
 * REST controller for user-related operations, such as registration
 * and Circle of Trust (connected users).
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /** The user service. */
    private final UserService userService;

    public UserController(final UserService theUserService) {
        Validate.notNull(theUserService, "the userService cannot be null.");
        userService = theUserService;
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
            Optional<User> userOpt = userService.findUserByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();
            return ResponseEntity.ok(toCurrentUserResponse(user));
        } catch (Exception e) {
            logger.error("Error getting current user: ", e);
            throw e;
        }
    }

    /**
     * Gets users in the current user's Circle of Trust.
     *
     * @param userDetails the authenticated user details
     * @return a response with the connected users' information
     */
    @GetMapping
    public ResponseEntity<UserListResponse> getConnectedUsers(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = requireCurrentUser(userDetails);
            List<User> connected = userService.getConnectedUsers(currentUser);
            List<UserResponse> userResponses = buildUserListWithCurrentUser(connected, currentUser);
            return ResponseEntity.ok(
                new UserListResponse("Connected users retrieved successfully", userResponses, userResponses.size()));
        } catch (Exception e) {
            logger.error("Error getting connected users: ", e);
            throw e;
        }
    }

    /**
     * Connects the current user with another user by email (Circle of Trust).
     *
     * @param userDetails the authenticated user details
     * @param request the connect request containing the email
     * @return a success message
     */
    @PostMapping("/connect")
    public ResponseEntity<RegisterResponse> connectUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ConnectUserRequest request) {
        try {
            User currentUser = requireCurrentUser(userDetails);
            userService.connectUsers(currentUser, request.email());
            return ResponseEntity.ok(new RegisterResponse("User connected successfully"));
        } catch (Exception e) {
            logger.error("Error connecting user: ", e);
            throw e;
        }
    }

    /**
     * Searches for a user by email (e.g. before connecting). Requires authentication.
     *
     * @param email the email to search for (query param)
     * @return the user information if found
     */
    @GetMapping("/search")
    public ResponseEntity<UserResponse> searchUserByEmail(@RequestParam("email") String email) {
        return userService.findUserByEmail(email)
            .map(user -> ResponseEntity.ok(toUserResponse(user)))
            .orElse(ResponseEntity.notFound().build());
    }

    private User requireCurrentUser(UserDetails userDetails) {
        return userService.getCurrentUser(userDetails.getUsername());
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail().getEmail(),
            user.getRoles().stream().map(Role::name).toArray(String[]::new),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private CurrentUserResponse toCurrentUserResponse(User user) {
        return new CurrentUserResponse(
            user.getId(),
            user.getName(),
            user.getEmail().getEmail(),
            user.getRoles().stream().map(Role::name).toArray(String[]::new),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private List<UserResponse> buildUserListWithCurrentUser(List<User> connectedUsers, User currentUser) {
        List<UserResponse> list = new ArrayList<>(connectedUsers.stream().map(this::toUserResponse).toList());
        boolean currentInList = connectedUsers.stream()
            .anyMatch(u -> u.getId().equals(currentUser.getId()));
        if (!currentInList) {
            list.add(toUserResponse(currentUser));
        }
        return list;
    }
}
