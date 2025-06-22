package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository implementation for Category entities.
 * Provides database operations for category persistence.
 */
@Repository
public interface CategoryJpaRepository extends JpaRepository<Category, Long> {
    
    /**
     * Finds a category by its name.
     *
     * @param name the category name. Cannot be null or blank.
     * @return an Optional containing the category if found, empty otherwise. Never null.
     */
    java.util.Optional<Category> findByName(String name);
    
    /**
     * Checks if a category exists by its name.
     *
     * @param name the category name. Cannot be null or blank.
     * @return true if the category exists, false otherwise.
     */
    boolean existsByName(String name);
} 