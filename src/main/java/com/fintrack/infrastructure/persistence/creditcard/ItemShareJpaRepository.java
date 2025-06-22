package com.fintrack.infrastructure.persistence.creditcard;

import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for ItemShare entities.
 * Provides data access methods for item share operations.
 */
@Repository
public interface ItemShareJpaRepository extends JpaRepository<ItemShare, Long> {

    /**
     * Finds all shares for a specific invoice item.
     *
     * @param invoiceItem the invoice item to find shares for. Must not be null.
     * @return a list of shares for the item. Never null, may be empty.
     */
    List<ItemShare> findByInvoiceItem(InvoiceItem invoiceItem);

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
    @Query("SELECT is FROM ItemShare is " +
           "JOIN is.invoiceItem ii " +
           "JOIN ii.invoice i " +
           "WHERE is.user = :user " +
           "AND YEAR(i.dueDate) = :year " +
           "AND MONTH(i.dueDate) = :month")
    List<ItemShare> findByUserAndMonth(@Param("user") User user, 
                                      @Param("year") int year, 
                                      @Param("month") int month);

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
} 