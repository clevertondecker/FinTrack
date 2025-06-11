package com.fintrack.controller.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for user login request.
 * This record is used to encapsulate the data required for user authentication.
 *
 * @param email the user's email address. Cannot be blank and must be a valid email format.
 *
 * @param password the user's password. Cannot be blank.
 */
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be in valid format.")
    String email,

    @NotBlank(message = "Password is required.")
    String password
) {}