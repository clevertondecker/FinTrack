package com.fintrack.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for connecting the current user to another user by email (Circle of Trust).
 */
public record ConnectUserRequest(
    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    String email
) {}
