package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryJpaRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Category> findAllByOrderByDisplayOrderAscNameAsc();
} 