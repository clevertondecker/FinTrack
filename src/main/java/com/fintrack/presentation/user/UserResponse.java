package com.fintrack.presentation.user;

import com.fintrack.domain.user.User;
import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String name,
    String email,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public UserResponse(User user) {
        this(user.getId(), user.getName(), user.getEmail().getValue(), user.getCreatedAt(), user.getUpdatedAt());
    }
}