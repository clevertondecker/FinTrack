package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
    String name,
    @Size(max = 20, message = "Color must be at most 20 characters")
    String color
) {} 