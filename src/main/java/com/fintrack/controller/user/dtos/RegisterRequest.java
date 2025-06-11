package com.fintrack.controller.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration request.
 *
 * @param name the name of the user. Cannot be blank and must be between 2
 * and 100 characters.
 *
 * @param email the email address of the user. Cannot be blank and must be a
 * valid email format.
 *
 * @param password the password for the user account. Cannot be blank and must
 * be at least 6 characters long.
 */
public record RegisterRequest(
  @NotBlank(message = "Name is required.")
  @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
  String name,

  @NotBlank(message = "Email is required.")
  @Email(message = "Email must be a valid format.")
  String email,

  @NotBlank(message = "Password is required.")
  @Size(min = 6, message = "Password must be at least 6 characters long.")
  String password
) {}
