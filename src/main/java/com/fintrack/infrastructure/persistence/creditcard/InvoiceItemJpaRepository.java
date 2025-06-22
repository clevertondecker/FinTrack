package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.InvoiceItemRepository;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository implementation for InvoiceItem entities.
 * Provides database operations for invoice item persistence.
 */
@Repository
public interface InvoiceItemJpaRepository extends JpaRepository<InvoiceItem, Long>, InvoiceItemRepository {

    /**
     * Finds an invoice item by its ID and invoice credit card owner.
     *
     * @param id the invoice item ID. Cannot be null.
     * @param user the owner of the credit card. Cannot be null.
     * @return an Optional containing the invoice item if found, empty otherwise. Never null.
     */
    @Query("SELECT ii FROM InvoiceItem ii " +
           "JOIN FETCH ii.invoice i " +
           "JOIN FETCH i.creditCard cc " +
           "WHERE ii.id = :id AND cc.owner = :user")
    Optional<InvoiceItem> findByIdAndInvoiceCreditCardOwner(@Param("id") Long id, @Param("user") User user);

    /**
     * Finds all items for a specific invoice.
     *
     * @param invoice the invoice. Cannot be null.
     * @return a list of items for the invoice. Never null, may be empty.
     */
    @Override
    List<InvoiceItem> findByInvoice(Invoice invoice);

    /**
     * Finds invoice items by category.
     *
     * @param category the category. Cannot be null.
     * @return a list of invoice items with the specified category. Never null, may be empty.
     */
    @Override
    List<InvoiceItem> findByCategory(Category category);

    /**
     * Finds invoice items by category name.
     *
     * @param categoryName the category name. Cannot be null or blank.
     * @return a list of invoice items with the specified category name. Never null, may be empty.
     */
    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.category.name = :categoryName")
    List<InvoiceItem> findByCategoryName(@Param("categoryName") String categoryName);

    /**
     * Finds invoice items by invoice and category.
     *
     * @param invoice the invoice. Cannot be null.
     * @param category the category. Cannot be null.
     * @return a list of invoice items with the specified category for the invoice. Never null, may be empty.
     */
    List<InvoiceItem> findByInvoiceAndCategory(Invoice invoice, Category category);

    /**
     * Finds invoice items by invoice and category name.
     *
     * @param invoice the invoice. Cannot be null.
     * @param categoryName the category name. Cannot be null or blank.
     * @return a list of invoice items with the specified category name for the invoice. Never null, may be empty.
     */
    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.invoice = :invoice AND ii.category.name = :categoryName")
    List<InvoiceItem> findByInvoiceAndCategoryName(@Param("invoice") Invoice invoice, @Param("categoryName") String categoryName);

    /**
     * Counts items by invoice.
     *
     * @param invoice the invoice. Cannot be null.
     * @return the number of items for the invoice.
     */
    long countByInvoice(Invoice invoice);

    // Implementações dos métodos da interface InvoiceItemRepository

    @Override
    default Optional<InvoiceItem> findByIdAndInvoice(Long id, Invoice invoice) {
        return findByInvoice(invoice).stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    @Override
    default List<InvoiceItem> findByUserShares(User user, YearMonth month) {
        // Esta implementação seria mais complexa e precisaria de uma query customizada
        // Por enquanto, retornamos uma lista vazia
        return List.of();
    }

    @Override
    default boolean existsByIdAndInvoice(Long id, Invoice invoice) {
        return findByIdAndInvoice(id, invoice).isPresent();
    }

    @Override
    default void deleteByIdAndInvoice(Long id, Invoice invoice) {
        findByIdAndInvoice(id, invoice).ifPresent(item -> delete(item));
    }
} 