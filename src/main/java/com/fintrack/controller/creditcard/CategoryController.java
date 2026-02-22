package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.CategoryService;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.dto.creditcard.CategoryListResponse;
import com.fintrack.dto.creditcard.CategoryCreateRequest;
import com.fintrack.dto.creditcard.CategoryCreateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

/**
 * REST controller for managing categories.
 * Provides endpoints for category operations.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    /** The category service. */
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Gets all categories.
     *
     * @return a response with all categories.
     */
    @GetMapping
    public ResponseEntity<CategoryListResponse> getAllCategories() {
        List<Category> categories = categoryService.findAll();
        return ResponseEntity.ok(CategoryListResponse.from(categories));
    }

    /**
     * Creates a new category.
     *
     * @param request the category creation request.
     * @return a response with the created category information.
     */
    @PostMapping
    public ResponseEntity<CategoryCreateResponse> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        Category savedCategory = categoryService.create(request.name().trim(), request.color());
        return ResponseEntity.ok(new CategoryCreateResponse(
            "Category created successfully",
            savedCategory.getId(),
            savedCategory.getName(),
            savedCategory.getColor()
        ));
    }
}