package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for InvoiceItem entities.
 * Provides methods to persist and retrieve invoice item data.
 */
public interface InvoiceItemRepository {

    /**
     * Saves an invoice item entity.
     *
     * @param invoiceItem the invoice item to save. Must not be null.
     * @return the saved invoice item entity. Never null.
     */
    InvoiceItem save(InvoiceItem invoiceItem);

    /**
     * Finds an invoice item by its ID and invoice.
     *
     * @param id the invoice item ID. Must not be null.
     * @param invoice the invoice the item belongs to. Must not be null.
     * @return an Optional containing the invoice item if found, empty otherwise.
     */
    Optional<InvoiceItem> findByIdAndInvoice(Long id, Invoice invoice);

    /**
     * Finds all items for a specific invoice.
     *
     * @param invoice the invoice to find items for. Must not be null.
     * @return a list of items for the invoice. Never null, may be empty.
     */
    List<InvoiceItem> findByInvoice(Invoice invoice);

    /**
     * Finds all items that have shares for a specific user and month.
     *
     * @param user the user to find items for. Must not be null.
     * @param month the month to find items for. Must not be null.
     * @return a list of items with shares for the user and month. Never null, may be empty.
     */
    List<InvoiceItem> findByUserShares(User user, YearMonth month);

    /**
     * Finds all items for a specific category.
     *
     * @param category the category to find items for. Must not be null.
     * @return a list of items for the category. Never null, may be empty.
     */
    List<InvoiceItem> findByCategory(Category category);

    /**
     * Checks if an invoice item exists by its ID and invoice.
     *
     * @param id the invoice item ID. Must not be null.
     * @param invoice the invoice the item belongs to. Must not be null.
     * @return true if the invoice item exists, false otherwise.
     */
    boolean existsByIdAndInvoice(Long id, Invoice invoice);

    /**
     * Deletes an invoice item by its ID and invoice.
     *
     * @param id the invoice item ID. Must not be null.
     * @param invoice the invoice the item belongs to. Must not be null.
     */
    void deleteByIdAndInvoice(Long id, Invoice invoice);
} 