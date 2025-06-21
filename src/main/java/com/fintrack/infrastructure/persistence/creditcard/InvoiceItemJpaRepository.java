package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository implementation for InvoiceItem entities.
 * Provides database operations for invoice item persistence.
 */
@Repository
public interface InvoiceItemJpaRepository extends JpaRepository<InvoiceItem, Long> {

    /**
     * Finds an invoice item by its ID and invoice credit card owner.
     *
     * @param id the invoice item ID.
     * @param user the owner of the credit card.
     * @return an Optional containing the invoice item if found, empty otherwise.
     */
    Optional<InvoiceItem> findByIdAndInvoiceCreditCardOwner(Long id, User user);

    /**
     * Finds all items for a specific invoice.
     *
     * @param invoice the invoice.
     * @return a list of items for the invoice.
     */
    List<InvoiceItem> findByInvoice(Invoice invoice);

    /**
     * Finds invoice items by category.
     *
     * @param category the category.
     * @return a list of invoice items with the specified category.
     */
    List<InvoiceItem> findByCategory(Category category);

    /**
     * Finds invoice items by category name.
     *
     * @param categoryName the category name.
     * @return a list of invoice items with the specified category name.
     */
    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.category.name = :categoryName")
    List<InvoiceItem> findByCategoryName(@Param("categoryName") String categoryName);

    /**
     * Finds invoice items by invoice and category.
     *
     * @param invoice the invoice.
     * @param category the category.
     * @return a list of invoice items with the specified category for the invoice.
     */
    List<InvoiceItem> findByInvoiceAndCategory(Invoice invoice, Category category);

    /**
     * Finds invoice items by invoice and category name.
     *
     * @param invoice the invoice.
     * @param categoryName the category name.
     * @return a list of invoice items with the specified category name for the invoice.
     */
    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.invoice = :invoice AND ii.category.name = :categoryName")
    List<InvoiceItem> findByInvoiceAndCategoryName(@Param("invoice") Invoice invoice, @Param("categoryName") String categoryName);

    /**
     * Counts items by invoice.
     *
     * @param invoice the invoice.
     * @return the number of items for the invoice.
     */
    long countByInvoice(Invoice invoice);
} 