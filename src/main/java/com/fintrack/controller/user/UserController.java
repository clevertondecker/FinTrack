package com.fintrack.controller.user;

import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.Role;
import com.fintrack.dto.user.RegisterRequest;
import com.fintrack.dto.user.RegisterResponse;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.infrastructure.persistence.user.UserJpaRepository;

import jakarta.validation.Valid;

/**
 * REST controller for user-related operations, such as registration.
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

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
}
