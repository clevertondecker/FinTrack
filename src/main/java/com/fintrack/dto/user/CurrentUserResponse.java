package com.fintrack.dto.user;

import java.time.LocalDateTime;

/**
 * DTO for current user response, including authentication provider.
 *
 * @param id the user's unique identifier
 * @param name the user's full name
 * @param email the user's email address
 * @param roles array of user roles
 * @param provider the authentication provider (LOCAL or GOOGLE)
 * @param createdAt timestamp when the user was created
 * @param updatedAt timestamp when the user was last updated
 */
public record CurrentUserResponse(
    Long id,
    String name,
    String email,
    String[] roles,
    String provider,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
