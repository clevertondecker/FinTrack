package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.contact.TrustedContact;
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
     * @param id the credit card ID. Cannot be null.
     * @param user the owner of the credit card. Cannot be null.
     * @return an Optional containing the credit card if found, empty otherwise. Never null.
     */
    Optional<CreditCard> findByIdAndOwner(Long id, User user);

    /**
     * Finds all credit cards owned by a specific user.
     *
     * @param user the user to find credit cards for. Cannot be null.
     * @return a list of credit cards owned by the user. Never null, may be empty.
     */
    List<CreditCard> findByOwner(User user);

    /**
     * Finds all credit cards the user can manage: owned by the user or whose parent card is owned by the user.
     *
     * @param user the user. Cannot be null.
     * @return list of credit cards. Never null, may be empty.
     */
    @Query("SELECT cc FROM CreditCard cc WHERE cc.owner = :user "
            + "OR (cc.parentCard IS NOT NULL AND cc.parentCard.owner = :user)")
    List<CreditCard> findByOwnerOrParentCardOwner(@Param("user") User user);

    /**
     * Finds all active credit cards the user can manage.
     *
     * @param user the user. Cannot be null.
     * @return list of active credit cards. Never null, may be empty.
     */
    @Query("SELECT cc FROM CreditCard cc WHERE cc.active = true AND (cc.owner = :user "
            + "OR (cc.parentCard IS NOT NULL AND cc.parentCard.owner = :user))")
    List<CreditCard> findByOwnerOrParentCardOwnerAndActiveTrue(@Param("user") User user);

    /**
     * Finds all active credit cards owned by a specific user.
     *
     * @param user the user to find active credit cards for. Cannot be null.
     * @return a list of active credit cards owned by the user. Never null, may be empty.
     */
    List<CreditCard> findByOwnerAndActiveTrue(User user);

    /**
     * Checks if a credit card exists by its ID and owner.
     *
     * @param id the credit card ID. Cannot be null.
     * @param user the owner of the credit card. Cannot be null.
     * @return true if the credit card exists, false otherwise.
     */
    boolean existsByIdAndOwner(Long id, User user);

    /**
     * Deletes a credit card by its ID and owner.
     *
     * @param id the credit card ID. Cannot be null.
     * @param user the owner of the credit card. Cannot be null.
     */
    void deleteByIdAndOwner(Long id, User user);

    /**
     * Finds credit cards by bank.
     *
     * @param bankId the bank ID. Cannot be null.
     * @return a list of credit cards from the specified bank. Never null, may be empty.
     */
    @Query("SELECT cc FROM CreditCard cc WHERE cc.bank.id = :bankId")
    List<CreditCard> findByBankId(@Param("bankId") Long bankId);

    /**
     * Counts credit cards by owner.
     *
     * @param user the owner. Cannot be null.
     * @return the number of credit cards owned by the user.
     */
    long countByOwner(User user);

    /**
     * Finds all credit cards assigned to a specific trusted contact.
     * Used during user registration to migrate contact assignments to user assignments.
     *
     * @param contact the trusted contact. Cannot be null.
     * @return list of credit cards assigned to the contact. Never null.
     */
    @Query("SELECT cc FROM CreditCard cc WHERE cc.assignedContact = :contact")
    List<CreditCard> findByAssignedContact(@Param("contact") TrustedContact contact);
} 