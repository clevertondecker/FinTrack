package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ItemShare entities.
 * Provides methods to persist and retrieve item share data.
 */
public interface ItemShareRepository {

    /**
     * Saves an item share entity.
     *
     * @param itemShare the item share to save. Must not be null.
     * @return the saved item share entity. Never null.
     */
    ItemShare save(ItemShare itemShare);

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
     * @param month the month to find shares for. Must not be null.
     * @return a list of shares for the user in the month. Never null, may be empty.
     */
    List<ItemShare> findByUserAndMonth(User user, YearMonth month);

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
    List<ItemShare> findUnpaidSharesByUser(User user);
} 