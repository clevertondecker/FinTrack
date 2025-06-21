package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CreditCard entities.
 * Provides methods to persist and retrieve credit card data.
 */
public interface CreditCardRepository {

    /**
     * Saves a credit card entity.
     *
     * @param creditCard the credit card to save. Must not be null.
     * @return the saved credit card entity. Never null.
     */
    CreditCard save(CreditCard creditCard);

    /**
     * Finds a credit card by its ID and owner.
     *
     * @param id the credit card ID. Must not be null.
     * @param user the owner of the credit card. Must not be null.
     * @return an Optional containing the credit card if found, empty otherwise.
     */
    Optional<CreditCard> findByIdAndUser(Long id, User user);

    /**
     * Finds all credit cards owned by a specific user.
     *
     * @param user the user to find credit cards for. Must not be null.
     * @return a list of credit cards owned by the user. Never null, may be empty.
     */
    List<CreditCard> findByUser(User user);

    /**
     * Finds all active credit cards owned by a specific user.
     *
     * @param user the user to find active credit cards for. Must not be null.
     * @return a list of active credit cards owned by the user. Never null, may be empty.
     */
    List<CreditCard> findByUserAndActiveTrue(User user);

    /**
     * Checks if a credit card exists by its ID and owner.
     *
     * @param id the credit card ID. Must not be null.
     * @param user the owner of the credit card. Must not be null.
     * @return true if the credit card exists, false otherwise.
     */
    boolean existsByIdAndUser(Long id, User user);

    /**
     * Deletes a credit card by its ID and owner.
     *
     * @param id the credit card ID. Must not be null.
     * @param user the owner of the credit card. Must not be null.
     */
    void deleteByIdAndUser(Long id, User user);
} 