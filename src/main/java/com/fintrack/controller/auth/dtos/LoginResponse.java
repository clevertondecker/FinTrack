package com.fintrack.controller.auth.dtos;

/**
 * DTO for user login response.
 *
 * @param token the JWT token generated upon successful login.
 * @param type the type of the token, typically "Bearer".
 */
public record LoginResponse(
    String token,
    String type
) {}