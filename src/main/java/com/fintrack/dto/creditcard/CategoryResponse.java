package com.fintrack.dto.creditcard;

import com.fintrack.domain.creditcard.Category;

/**
 * DTO representing a category in expense reports.
 *
 * @param id the category's unique identifier (null for uncategorized)
 * @param name the category's name
 * @param color the category's color in hexadecimal format
 */
public record CategoryResponse(Long id, String name, String color) {
    /**
     * Creates a CategoryResponse from a Category domain entity.
     *
     * @param category the category to convert. Can be null for uncategorized items.
     * @return a CategoryResponse. Never null.
     */
    public static CategoryResponse from(final Category category) {
        if (category == null) {
            return new CategoryResponse(null, "Sem categoria", "#CCCCCC");
        }
        return new CategoryResponse(category.getId(), category.getName(), category.getColor());
    }
} 