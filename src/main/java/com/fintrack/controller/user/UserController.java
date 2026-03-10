package com.fintrack.controller.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.Validate;
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
import com.fintrack.dto.user.ChangePasswordRequest;
import com.fintrack.dto.user.ConnectUserRequest;
import com.fintrack.dto.user.CurrentUserResponse;
import com.fintrack.dto.user.RegisterRequest;
import com.fintrack.dto.user.RegisterResponse;
import com.fintrack.dto.user.UserListResponse;
import com.fintrack.dto.user.UserResponse;

import jakarta.validation.Valid;

/**
 * REST controller for user-related operations: registration,
 * profile, password management, and Circle of Trust (connected users).
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(final UserService theUserService) {
        Validate.notNull(theUserService, "the userService cannot be null.");
        this.userService = theUserService;
    }

    /**
     * Registers a new user with the default USER role.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        userService.registerUser(request.name(), request.email(), request.password(), Set.of(Role.USER));
        return ResponseEntity.ok(new RegisterResponse("User registered successfully"));
    }

    /**
     * Gets the current authenticated user's information including provider.
     */
    @GetMapping("/current-user")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userOpt = userService.findUserByEmail(userDetails.getUsername());
        return userOpt.map(user -> ResponseEntity.ok(toCurrentUserResponse(user)))
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Gets users in the current user's Circle of Trust.
     */
    @GetMapping
    public ResponseEntity<UserListResponse> getConnectedUsers(
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = requireCurrentUser(userDetails);
        List<User> connected = userService.getConnectedUsers(currentUser);
        List<UserResponse> userResponses = buildUserListWithCurrentUser(connected, currentUser);
        return ResponseEntity.ok(
            new UserListResponse("Connected users retrieved successfully",
                                 userResponses, userResponses.size()));
    }

    /**
     * Connects the current user with another user by email (Circle of Trust).
     */
    @PostMapping("/connect")
    public ResponseEntity<RegisterResponse> connectUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ConnectUserRequest request) {
        User currentUser = requireCurrentUser(userDetails);
        userService.connectUsers(currentUser, request.email());
        return ResponseEntity.ok(new RegisterResponse("User connected successfully"));
    }

    /**
     * Searches for a user by email. Requires authentication.
     */
    @GetMapping("/search")
    public ResponseEntity<UserResponse> searchUserByEmail(@RequestParam("email") String email) {
        return userService.findUserByEmail(email)
            .map(user -> ResponseEntity.ok(toUserResponse(user)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Changes the password for the authenticated LOCAL user.
     *
     * @return 204 No Content on success
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        User user = requireCurrentUser(userDetails);
        userService.changePassword(user, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private User requireCurrentUser(UserDetails userDetails) {
        return userService.getCurrentUser(userDetails.getUsername());
    }

    private String[] mapRoles(User user) {
        return user.getRoles().stream().map(Role::name).toArray(String[]::new);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail().getEmail(),
            mapRoles(user),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private CurrentUserResponse toCurrentUserResponse(User user) {
        return new CurrentUserResponse(
            user.getId(),
            user.getName(),
            user.getEmail().getEmail(),
            mapRoles(user),
            user.getProvider().name(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private List<UserResponse> buildUserListWithCurrentUser(
            List<User> connectedUsers, User currentUser) {
        List<UserResponse> list = new ArrayList<>(
            connectedUsers.stream().map(this::toUserResponse).toList());
        boolean currentInList = connectedUsers.stream()
            .anyMatch(u -> u.getId().equals(currentUser.getId()));
        if (!currentInList) {
            list.add(toUserResponse(currentUser));
        }
        return list;
    }
}
