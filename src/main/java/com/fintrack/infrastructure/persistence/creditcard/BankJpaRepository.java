package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository implementation for Bank entities.
 * Provides database operations for bank persistence.
 */
@Repository
public interface BankJpaRepository extends JpaRepository<Bank, Long> {

    /**
     * Finds a bank by its code.
     *
     * @param code the bank code. Cannot be null or blank.
     * @return an Optional containing the bank if found, empty otherwise. Never null.
     */
    Optional<Bank> findByCode(String code);

    /**
     * Checks if a bank exists by its code.
     *
     * @param code the bank code. Cannot be null or blank.
     * @return true if the bank exists, false otherwise.
     */
    boolean existsByCode(String code);
} 