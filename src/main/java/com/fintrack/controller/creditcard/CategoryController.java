package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.CategoryService;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.dto.creditcard.CategoryCreateRequest;
import com.fintrack.dto.creditcard.CategoryCreateResponse;
import com.fintrack.dto.creditcard.CategoryListResponse;
import com.fintrack.dto.creditcard.CategoryMergeRequest;
import com.fintrack.dto.creditcard.CategoryReorderRequest;
import com.fintrack.dto.creditcard.CategoryResponse;
import com.fintrack.dto.creditcard.CategoryUpdateRequest;
import com.fintrack.dto.creditcard.CategoryUsageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<CategoryListResponse> getAllCategories() {
        List<Category> categories = categoryService.findAll();
        return ResponseEntity.ok(CategoryListResponse.from(categories));
    }

    @PostMapping
    public ResponseEntity<CategoryCreateResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        Category saved = categoryService.create(request.name().trim(), request.color(), request.icon());
        return ResponseEntity.ok(new CategoryCreateResponse(
                "Category created successfully",
                saved.getId(), saved.getName(), saved.getColor(),
                saved.getIcon(), saved.getDisplayOrder()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        Category updated = categoryService.update(id, request.name(), request.color(), request.icon());
        return ResponseEntity.ok(CategoryResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
    }

    @PostMapping("/merge")
    public ResponseEntity<CategoryResponse> mergeCategories(
            @Valid @RequestBody CategoryMergeRequest request) {
        Category target = categoryService.merge(request.sourceCategoryId(), request.targetCategoryId());
        return ResponseEntity.ok(CategoryResponse.from(target));
    }

    @PutMapping("/reorder")
    public ResponseEntity<Map<String, String>> reorderCategories(
            @Valid @RequestBody CategoryReorderRequest request) {
        categoryService.reorder(request.orderedIds());
        return ResponseEntity.ok(Map.of("message", "Categories reordered successfully"));
    }

    @GetMapping("/{id}/usage")
    public ResponseEntity<CategoryUsageResponse> getCategoryUsage(@PathVariable Long id) {
        CategoryUsageResponse usage = categoryService.getUsage(id);
        return ResponseEntity.ok(usage);
    }
}
