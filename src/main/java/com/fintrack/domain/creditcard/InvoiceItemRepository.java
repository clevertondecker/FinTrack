package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for InvoiceItem entities.
 * Provides methods to persist and retrieve invoice item data.
 */
@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    /**
     * Finds an invoice item by its ID and invoice.
     *
     * @param id the invoice item ID. Must not be null.
     * @param invoice the invoice the item belongs to. Must not be null.
     * @return an Optional containing the invoice item if found, empty otherwise.
     */
    Optional<InvoiceItem> findByIdAndInvoice(Long id, Invoice invoice);

    /**
     * Finds an invoice item by its ID and invoice credit card owner.
     *
     * @param id the invoice item ID. Must not be null.
     * @param user the owner of the credit card. Must not be null.
     * @return an Optional containing the invoice item if found, empty otherwise.
     */
    Optional<InvoiceItem> findByIdAndInvoiceCreditCardOwner(Long id, User user);

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
    @Query("SELECT DISTINCT ii FROM InvoiceItem ii "
        + "JOIN ii.shares s "
        + "JOIN ii.invoice i "
        + "WHERE s.user = :user "
        + "AND i.month = :month")
    List<InvoiceItem> findByUserShares(@Param("user") User user, 
                                      @Param("year") int year, 
                                      @Param("month") int month);

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

    /**
     * Convenience method to find items by user shares using YearMonth.
     *
     * @param user the user to find items for. Must not be null.
     * @param month the month to find items for. Must not be null.
     * @return a list of items with shares for the user and month. Never null, may be empty.
     */
    default List<InvoiceItem> findByUserShares(User user, YearMonth month) {
        return findByUserSharesYearMonth(user, month);
    }

    /**
     * Finds all items that have shares for a specific user and month using YearMonth.
     * Filters by invoice month field, not due date.
     *
     * @param user the user to find items for. Must not be null.
     * @param month the month to find items for. Must not be null.
     * @return a list of items with shares for the user and month. Never null, may be empty.
     */
    @Query("SELECT DISTINCT ii FROM InvoiceItem ii "
        + "JOIN ii.shares s "
        + "JOIN ii.invoice i "
        + "WHERE s.user = :user "
        + "AND i.month = :month")
    List<InvoiceItem> findByUserSharesYearMonth(@Param("user") User user, 
                                                @Param("month") YearMonth month);

    /**
     * Finds all invoice items for a specific card owner in a given month.
     * This method retrieves all items and the service layer will filter for unshared amounts.
     *
     * @param owner the card owner to find items for. Must not be null.
     * @param year the year to find items for.
     * @param month the month to find items for.
     * @return a list of invoice items for the owner in the month. Never null, may be empty.
     */
    /**
     * Finds all invoice items for a specific card owner in a given month.
     * Filters by invoice month field, not due date.
     * Note: This method signature is kept for backward compatibility but the query uses YearMonth.
     * The default method findItemsByOwnerAndMonth(YearMonth) should be used instead.
     *
     * @param owner the card owner to find items for. Must not be null.
     * @param year the year (not used, kept for compatibility).
     * @param month the month (not used, kept for compatibility).
     * @return a list of invoice items for the owner in the month. Never null, may be empty.
     * @deprecated Use findItemsByOwnerAndMonth(User, YearMonth) instead.
     */
    @Deprecated
    default List<InvoiceItem> findItemsByOwnerAndMonth(@Param("owner") User owner,
                                               @Param("year") int year,
                                               @Param("month") int month) {
        return findItemsByOwnerAndMonthYearMonth(owner, YearMonth.of(year, month));
    }

    /**
     * Convenience method to find items by owner and month using YearMonth.
     *
     * @param owner the card owner to find items for. Must not be null.
     * @param month the month to find items for. Must not be null.
     * @return a list of invoice items for the owner in the month. Never null, may be empty.
     */
    default List<InvoiceItem> findItemsByOwnerAndMonth(User owner, YearMonth month) {
        return findItemsByOwnerAndMonthYearMonth(owner, month);
    }

    /**
     * Finds all invoice items for a specific card owner in a given month using YearMonth.
     * Filters by invoice month field, not due date.
     *
     * @param owner the card owner to find items for. Must not be null.
     * @param month the month to find items for. Must not be null.
     * @return a list of invoice items for the owner in the month. Never null, may be empty.
     */
    @Query("SELECT DISTINCT ii FROM InvoiceItem ii "
        + "LEFT JOIN FETCH ii.category "
        + "JOIN ii.invoice i "
        + "JOIN i.creditCard cc "
        + "WHERE cc.owner = :owner "
        + "AND i.month = :month")
    List<InvoiceItem> findItemsByOwnerAndMonthYearMonth(@Param("owner") User owner,
                                                       @Param("month") YearMonth month);
} 