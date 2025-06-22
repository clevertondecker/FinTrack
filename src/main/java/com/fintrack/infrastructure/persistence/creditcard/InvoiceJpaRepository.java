package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceRepository;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository implementation for Invoice entities.
 * Provides database operations for invoice persistence.
 */
@Repository
public interface InvoiceJpaRepository extends JpaRepository<Invoice, Long>, InvoiceRepository {

    /**
     * Finds an invoice by its ID and credit card owner.
     *
     * @param id the invoice ID. Cannot be null.
     * @param user the owner of the credit card. Cannot be null.
     * @return an Optional containing the invoice if found, empty otherwise. Never null.
     */
    Optional<Invoice> findByIdAndCreditCardOwner(Long id, User user);

    /**
     * Finds all invoices for a specific credit card.
     *
     * @param creditCard the credit card. Cannot be null.
     * @return a list of invoices for the credit card. Never null, may be empty.
     */
    @Override
    List<Invoice> findByCreditCard(CreditCard creditCard);

    /**
     * Finds all invoices for credit cards owned by a specific user.
     *
     * @param user the owner of the credit cards. Cannot be null.
     * @return a list of invoices for the user's credit cards. Never null, may be empty.
     */
    @Query("SELECT i FROM Invoice i WHERE i.creditCard.owner = :user")
    List<Invoice> findByCreditCardOwner(@Param("user") User user);

    /**
     * Finds invoices by credit card and status.
     *
     * @param creditCard the credit card. Cannot be null.
     * @param status the invoice status. Cannot be null.
     * @return a list of invoices with the specified status. Never null, may be empty.
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

    // Implementações dos métodos da interface InvoiceRepository

    @Override
    default Optional<Invoice> findByIdAndCreditCard(Long id, CreditCard creditCard) {
        return findByCreditCard(creditCard).stream()
                .filter(invoice -> invoice.getId().equals(id))
                .findFirst();
    }

    @Override
    default List<Invoice> findByCreditCardAndMonth(CreditCard creditCard, YearMonth month) {
        return findByCreditCard(creditCard).stream()
                .filter(invoice -> invoice.getMonth().equals(month))
                .toList();
    }

    @Override
    default List<Invoice> findByMonth(YearMonth month) {
        return findAll().stream()
                .filter(invoice -> invoice.getMonth().equals(month))
                .toList();
    }

    @Override
    default boolean existsByIdAndCreditCard(Long id, CreditCard creditCard) {
        return findByIdAndCreditCard(id, creditCard).isPresent();
    }

    @Override
    default void deleteByIdAndCreditCard(Long id, CreditCard creditCard) {
        findByIdAndCreditCard(id, creditCard).ifPresent(this::delete);
    }
} 