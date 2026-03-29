package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
    @NotBlank(message = "Category name is required")
    String name,
    @Size(max = 20, message = "Color must be at most 20 characters")
    String color,
    @Size(max = 50, message = "Icon must be at most 50 characters")
    String icon
) {}
