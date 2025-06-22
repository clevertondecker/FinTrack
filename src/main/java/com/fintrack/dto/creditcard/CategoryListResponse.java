package com.fintrack.dto.creditcard;

import com.fintrack.domain.creditcard.Category;
import java.util.List;
import java.util.stream.Collectors;

public record CategoryListResponse(String message, List<CategoryResponse> categories, int count) {
    public static CategoryListResponse from(List<Category> categories) {
        List<CategoryResponse> categoryResponses = categories.stream()
            .map(CategoryResponse::from)
            .collect(Collectors.toList());
        return new CategoryListResponse("Categories retrieved successfully", categoryResponses, categoryResponses.size());
    }
} 