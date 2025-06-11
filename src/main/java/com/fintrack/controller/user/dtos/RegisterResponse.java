package com.fintrack.controller.user.dtos;

/**
 * DTO for user registration response.
 *
 * @param message a success message indicating the result of the registration
 * operation.
 */
public record RegisterResponse(String message) {}