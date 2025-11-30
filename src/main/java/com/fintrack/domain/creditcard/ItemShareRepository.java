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
 * Repository interface for ItemShare entities.
 * Provides methods to persist and retrieve item share data.
 */
@Repository
public interface ItemShareRepository extends JpaRepository<ItemShare, Long> {

    /**
     * Finds all shares for a specific invoice item.
     *
     * @param invoiceItem the invoice item to find shares for. Must not be null.
     * @return a list of shares for the item. Never null, may be empty.
     */
    @Query("SELECT is FROM ItemShare is " +
           "JOIN FETCH is.user " +
           "JOIN FETCH is.invoiceItem ii " +
           "JOIN FETCH ii.invoice i " +
           "WHERE is.invoiceItem = :invoiceItem")
    List<ItemShare> findByInvoiceItem(@Param("invoiceItem") InvoiceItem invoiceItem);

    /**
     * Finds all shares for a specific user.
     *
     * @param user the user to find shares for. Must not be null.
     * @return a list of shares for the user. Never null, may be empty.
     */
    List<ItemShare> findByUser(User user);

    /**
     * Finds all shares for a specific user in a given month.
     *
     * @param user the user to find shares for. Must not be null.
     * @param year the year to find shares for.
     * @param month the month to find shares for.
     * @return a list of shares for the user in the month. Never null, may be empty.
     */
    /**
     * Finds all shares for a specific user in a given month.
     * Filters by invoice month field, not due date.
     * Note: This method signature is kept for backward compatibility but the query uses YearMonth.
     * The default method findByUserAndMonth(YearMonth) should be used instead.
     *
     * @param user the user to find shares for. Must not be null.
     * @param year the year (not used, kept for compatibility).
     * @param month the month (not used, kept for compatibility).
     * @return a list of shares for the user in the month. Never null, may be empty.
     * @deprecated Use findByUserAndMonth(User, YearMonth) instead.
     */
    @Deprecated
    default List<ItemShare> findByUserAndMonth(@Param("user") User user, 
                                      @Param("year") int year, 
                                      @Param("month") int month) {
        return findByUserAndMonthWithYearMonth(user, YearMonth.of(year, month));
    }

    /**
     * Finds a share by user and invoice item.
     *
     * @param user the user. Must not be null.
     * @param invoiceItem the invoice item. Must not be null.
     * @return an Optional containing the share if found, empty otherwise.
     */
    Optional<ItemShare> findByUserAndInvoiceItem(User user, InvoiceItem invoiceItem);

    /**
     * Finds all shares where the user is responsible for payment.
     *
     * @param user the user to find responsible shares for. Must not be null.
     * @return a list of shares where the user is responsible. Never null, may be empty.
     */
    List<ItemShare> findByUserAndResponsibleTrue(User user);

    /**
     * Counts the number of shares for a specific invoice item.
     *
     * @param invoiceItem the invoice item to count shares for. Must not be null.
     * @return the number of shares for the item.
     */
    long countByInvoiceItem(InvoiceItem invoiceItem);

    /**
     * Deletes all shares for a specific invoice item.
     *
     * @param invoiceItem the invoice item to delete shares for. Must not be null.
     */
    void deleteByInvoiceItem(InvoiceItem invoiceItem);

    /**
     * Finds all shares for a specific user that are not yet paid.
     *
     * @param user the user to find unpaid shares for. Must not be null.
     * @return a list of unpaid shares for the user. Never null, may be empty.
     */
    @Query("SELECT is FROM ItemShare is " +
           "WHERE is.user = :user " +
           "AND is.responsible = true")
    List<ItemShare> findUnpaidSharesByUser(@Param("user") User user);

    /**
     * Convenience method to find shares by user and month using YearMonth.
     *
     * @param user the user to find shares for. Must not be null.
     * @param month the month to find shares for. Must not be null.
     * @return a list of shares for the user in the month. Never null, may be empty.
     */
    default List<ItemShare> findByUserAndMonth(User user, YearMonth month) {
        return findByUserAndMonthWithYearMonth(user, month);
    }

    /**
     * Finds all shares for a specific user in a given month using YearMonth.
     * Filters by invoice month field, not due date.
     *
     * @param user the user to find shares for. Must not be null.
     * @param month the month to find shares for. Must not be null.
     * @return a list of shares for the user in the month. Never null, may be empty.
     */
    @Query("SELECT is FROM ItemShare is " +
           "JOIN is.invoiceItem ii " +
           "JOIN ii.invoice i " +
           "WHERE is.user = :user " +
           "AND i.month = :month")
    List<ItemShare> findByUserAndMonthWithYearMonth(@Param("user") User user,
                                                    @Param("month") YearMonth month);

    /**
     * Finds all shares for a specific user in a given month, including category information.
     * Uses JOIN FETCH to eagerly load category data for performance.
     *
     * @param user the user to find shares for. Must not be null.
     * @param month the month to find shares for. Must not be null.
     * @return a list of shares for the user in the month with category information loaded. Never null, may be empty.
     */
    /**
     * Finds all shares for a specific user in a given month with category information using YearMonth.
     * Filters by invoice month field, not due date.
     *
     * @param user the user to find shares for. Must not be null.
     * @param month the month to find shares for. Must not be null.
     * @return a list of shares for the user in the month with category information. Never null, may be empty.
     */
    @Query("SELECT is FROM ItemShare is " +
           "JOIN FETCH is.invoiceItem ii " +
           "LEFT JOIN FETCH ii.category " +
           "JOIN ii.invoice i " +
           "WHERE is.user = :user " +
           "AND i.month = :month")
    List<ItemShare> findByUserAndMonthWithCategory(@Param("user") User user,
                                                   @Param("month") YearMonth month);
} 