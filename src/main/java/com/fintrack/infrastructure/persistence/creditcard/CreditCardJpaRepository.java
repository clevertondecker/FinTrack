package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository implementation for CreditCard entities.
 * Provides database operations for credit card persistence.
 */
@Repository
public interface CreditCardJpaRepository extends JpaRepository<CreditCard, Long> {

    /**
     * Finds a credit card by its ID and owner.
     *
     * @param id the credit card ID.
     * @param user the owner of the credit card.
     * @return an Optional containing the credit card if found, empty otherwise.
     */
    Optional<CreditCard> findByIdAndOwner(Long id, User user);

    /**
     * Finds all credit cards owned by a specific user.
     *
     * @param user the user to find credit cards for.
     * @return a list of credit cards owned by the user.
     */
    List<CreditCard> findByOwner(User user);

    /**
     * Finds all active credit cards owned by a specific user.
     *
     * @param user the user to find active credit cards for.
     * @return a list of active credit cards owned by the user.
     */
    List<CreditCard> findByOwnerAndActiveTrue(User user);

    /**
     * Checks if a credit card exists by its ID and owner.
     *
     * @param id the credit card ID.
     * @param user the owner of the credit card.
     * @return true if the credit card exists, false otherwise.
     */
    boolean existsByIdAndOwner(Long id, User user);

    /**
     * Deletes a credit card by its ID and owner.
     *
     * @param id the credit card ID.
     * @param user the owner of the credit card.
     */
    void deleteByIdAndOwner(Long id, User user);

    /**
     * Finds credit cards by bank.
     *
     * @param bankId the bank ID.
     * @return a list of credit cards from the specified bank.
     */
    @Query("SELECT cc FROM CreditCard cc WHERE cc.bank.id = :bankId")
    List<CreditCard> findByBankId(@Param("bankId") Long bankId);

    /**
     * Counts credit cards by owner.
     *
     * @param user the owner.
     * @return the number of credit cards owned by the user.
     */
    long countByOwner(User user);
} 