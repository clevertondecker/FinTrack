package com.fintrack.controller.creditcard;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
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

    /** The category repository. */
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
    public ResponseEntity<CategoryListResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
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
        Category category = Category.of(request.name().trim(), request.color());
        Category savedCategory = categoryRepository.save(category);
        return ResponseEntity.ok(new CategoryCreateResponse(
            "Category created successfully",
            savedCategory.getId(),
            savedCategory.getName(),
            savedCategory.getColor()
        ));
    }
}