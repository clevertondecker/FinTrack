package com.fintrack.dto.creditcard;

import com.fintrack.domain.creditcard.Category;

public record CategoryResponse(Long id, String name, String color) {
    public static CategoryResponse from(Category category) {
        if (category == null) {
            return new CategoryResponse(null, "Sem categoria", "#CCCCCC");
        }
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }
} 