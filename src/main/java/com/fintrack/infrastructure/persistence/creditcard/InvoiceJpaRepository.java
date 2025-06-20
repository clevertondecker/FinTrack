package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository implementation for Invoice entities.
 * Provides database operations for invoice persistence.
 */
@Repository
public interface InvoiceJpaRepository extends JpaRepository<Invoice, Long> {

    /**
     * Finds an invoice by its ID and credit card owner.
     *
     * @param id the invoice ID.
     * @param user the owner of the credit card.
     * @return an Optional containing the invoice if found, empty otherwise.
     */
    Optional<Invoice> findByIdAndCreditCardOwner(Long id, User user);

    /**
     * Finds all invoices for a specific credit card.
     *
     * @param creditCard the credit card.
     * @return a list of invoices for the credit card.
     */
    List<Invoice> findByCreditCard(CreditCard creditCard);

    /**
     * Finds all invoices for credit cards owned by a specific user.
     *
     * @param user the owner of the credit cards.
     * @return a list of invoices for the user's credit cards.
     */
    @Query("SELECT i FROM Invoice i WHERE i.creditCard.owner = :user")
    List<Invoice> findByCreditCardOwner(@Param("user") User user);

    /**
     * Finds invoices by credit card and status.
     *
     * @param creditCard the credit card.
     * @param status the invoice status.
     * @return a list of invoices with the specified status.
     */
    List<Invoice> findByCreditCardAndStatus(CreditCard creditCard, com.fintrack.domain.creditcard.InvoiceStatus status);

    /**
     * Counts invoices by credit card.
     *
     * @param creditCard the credit card.
     * @return the number of invoices for the credit card.
     */
    long countByCreditCard(CreditCard creditCard);

    /**
     * Finds all invoices for a specific credit card.
     *
     * @param creditCardId the credit card ID.
     * @return a list of invoices for the credit card.
     */
    List<Invoice> findByCreditCardId(Long creditCardId);

    /**
     * Finds all invoices for a specific user.
     *
     * @param user the user to find invoices for.
     * @return a list of invoices for the user.
     */
    @Query("SELECT i FROM Invoice i WHERE i.creditCard.owner = :user")
    List<Invoice> findByUser(@Param("user") User user);

    /**
     * Updates the total amount of an invoice based on its items.
     *
     * @param invoiceId the invoice ID.
     */
    @Modifying
    @Query("UPDATE Invoice i SET i.totalAmount = (SELECT COALESCE(SUM(ii.amount), 0) FROM InvoiceItem ii WHERE ii.invoice.id = :invoiceId) WHERE i.id = :invoiceId")
    void updateTotalAmount(@Param("invoiceId") Long invoiceId);
} 