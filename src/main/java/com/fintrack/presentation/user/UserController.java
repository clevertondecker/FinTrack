package com.fintrack.presentation.user;

import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.User;

import org.apache.commons.lang3.Validate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(final UserService theUserService) {
        Validate.notNull(theUserService, "the userService cannot be null.");
        userService = theUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody @Validated UserRegistrationRequest request) {
        User user =
          userService.registerUser(request.name(), request.email(), request.password());

        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(user));
    }
}