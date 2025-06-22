package com.fintrack.dto.user;

import java.util.List;

/**
 * DTO for user list response.
 *
 * @param message the response message
 * @param users list of user information
 * @param count total number of users
 */
public record UserListResponse(
    String message,
    List<UserResponse> users,
    int count
) {} 