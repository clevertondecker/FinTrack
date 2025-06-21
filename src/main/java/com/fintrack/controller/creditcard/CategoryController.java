package com.fintrack.controller.creditcard;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * REST controller for managing categories.
 * Provides endpoints for category operations.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryJpaRepository categoryRepository;

    public CategoryController(CategoryJpaRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Gets all categories.
     *
     * @return a response with all categories.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();

        List<Map<String, Object>> categoryDtos = new ArrayList<>();
        for (Category category : categories) {
            Map<String, Object> categoryDto = new HashMap<>();
            categoryDto.put("id", category.getId());
            categoryDto.put("name", category.getName());
            categoryDto.put("color", category.getColor());
            categoryDtos.add(categoryDto);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Categories retrieved successfully");
        response.put("categories", categoryDtos);
        response.put("count", categoryDtos.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new category.
     *
     * @param request the category creation request.
     * @return a response with the created category information.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String color = request.get("color");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Category name is required"));
        }

        Category category = Category.of(name.trim(), color);
        Category savedCategory = categoryRepository.save(category);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Category created successfully");
        response.put("id", savedCategory.getId());
        response.put("name", savedCategory.getName());
        response.put("color", savedCategory.getColor());

        return ResponseEntity.ok(response);
    }
} 