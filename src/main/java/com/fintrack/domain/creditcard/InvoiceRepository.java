package com.fintrack.domain.creditcard;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Invoice entities.
 * Provides methods to persist and retrieve invoice data.
 */
public interface InvoiceRepository {

    /**
     * Saves an invoice entity.
     *
     * @param invoice the invoice to save. Must not be null.
     * @return the saved invoice entity. Never null.
     */
    Invoice save(Invoice invoice);

    /**
     * Finds an invoice by its ID and credit card.
     *
     * @param id the invoice ID. Must not be null.
     * @param creditCard the credit card the invoice belongs to. Must not be null.
     * @return an Optional containing the invoice if found, empty otherwise.
     */
    Optional<Invoice> findByIdAndCreditCard(Long id, CreditCard creditCard);

    /**
     * Finds all invoices for a specific credit card and month.
     *
     * @param creditCard the credit card to find invoices for. Must not be null.
     * @param month the month to find invoices for. Must not be null.
     * @return a list of invoices for the credit card and month. Never null, may be empty.
     */
    List<Invoice> findByCreditCardAndMonth(CreditCard creditCard, YearMonth month);

    /**
     * Finds all invoices for a specific credit card.
     *
     * @param creditCard the credit card to find invoices for. Must not be null.
     * @return a list of invoices for the credit card. Never null, may be empty.
     */
    List<Invoice> findByCreditCard(CreditCard creditCard);

    /**
     * Finds all invoices for a specific month across all credit cards.
     *
     * @param month the month to find invoices for. Must not be null.
     * @return a list of invoices for the month. Never null, may be empty.
     */
    List<Invoice> findByMonth(YearMonth month);

    /**
     * Checks if an invoice exists by its ID and credit card.
     *
     * @param id the invoice ID. Must not be null.
     * @param creditCard the credit card the invoice belongs to. Must not be null.
     * @return true if the invoice exists, false otherwise.
     */
    boolean existsByIdAndCreditCard(Long id, CreditCard creditCard);

    /**
     * Deletes an invoice by its ID and credit card.
     *
     * @param id the invoice ID. Must not be null.
     * @param creditCard the credit card the invoice belongs to. Must not be null.
     */
    void deleteByIdAndCreditCard(Long id, CreditCard creditCard);
} 