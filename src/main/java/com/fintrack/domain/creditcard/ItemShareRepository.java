package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;

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
    @Query("SELECT is FROM ItemShare is "
        + "JOIN FETCH is.user "
        + "JOIN FETCH is.invoiceItem ii "
        + "JOIN FETCH ii.invoice i "
        + "WHERE is.invoiceItem = :invoiceItem")
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
     * Filters by invoice month field, not due date.
     * Note: This method signature is kept for backward compatibility but the query uses YearMonth.
     * The default method findByUserAndMonth(YearMonth) should be used instead.
     *
     * @param user the user to find shares for. Must not be null.
     * @param year the year (not used, kept for compatibility).
     * @param month the month (not used, kept for compatibility).
     *
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
    @Query("SELECT is FROM ItemShare is "
        + "JOIN is.invoiceItem ii "
        + "JOIN ii.invoice i "
        + "WHERE is.user = :user "
        + "AND i.month = :month")
    List<ItemShare> findByUserAndMonthWithYearMonth(@Param("user") User user,
                                                    @Param("month") YearMonth month);
} 