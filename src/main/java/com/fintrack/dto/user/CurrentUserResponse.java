package com.fintrack.dto.user;

import java.time.LocalDateTime;

/**
 * DTO for current user response.
 *
 * @param id the user's unique identifier
 * @param name the user's full name
 * @param email the user's email address
 * @param roles array of user roles
 * @param createdAt timestamp when the user was created
 * @param updatedAt timestamp when the user was last updated
 */
public record CurrentUserResponse(
    Long id,
    String name,
    String email,
    String[] roles,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {} 