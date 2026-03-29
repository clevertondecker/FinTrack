package com.fintrack.dto.creditcard;

import com.fintrack.domain.creditcard.Category;

/**
 * DTO representing a category.
 *
 * @param id the category's unique identifier (null for uncategorized)
 * @param name the category's name
 * @param color the category's color in hexadecimal format
 * @param icon the category's icon identifier
 * @param displayOrder the category's display order
 */
public record CategoryResponse(Long id, String name, String color, String icon, Integer displayOrder) {

    public static CategoryResponse from(final Category category) {
        if (category == null) {
            return new CategoryResponse(null, "Sem categoria", "#CCCCCC", null, null);
        }
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getColor(),
            category.getIcon(),
            category.getDisplayOrder()
        );
    }
}
