package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for category operations.
 * Keeps controllers independent of infrastructure (fintrack-architecture).
 */
@Service
public class CategoryService {

    private final CategoryJpaRepository categoryRepository;

    public CategoryService(CategoryJpaRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns all categories.
     *
     * @return list of all categories. Never null.
     */
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    /**
     * Creates a new category.
     *
     * @param name  the category name. Cannot be null or blank.
     * @param color the optional color (e.g. hex). May be null.
     * @return the saved category. Never null.
     */
    @Transactional
    public Category create(String name, String color) {
        Category category = Category.of(name, color);
        return categoryRepository.save(category);
    }
}
