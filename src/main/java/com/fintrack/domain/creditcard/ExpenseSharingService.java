package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Domain service for managing expense sharing between users.
 * Provides business logic for sharing invoice items among users.
 */
public interface ExpenseSharingService {

    /**
     * Shares an invoice item among multiple users with specified percentages.
     *
     * @param item the invoice item to share. Must not be null.
     *
     * @param shares a map of users to their share percentages (0.0 to 1.0). Must not be null.
     *
     * @throws IllegalArgumentException if the sum of percentages is not 1.0 or
     * if any percentage is invalid.
     */
    void shareItem(InvoiceItem item, Map<User, BigDecimal> shares);

    /**
     * Updates the shares for an existing invoice item.
     *
     * @param item the invoice item to update shares for. Must not be null.
     *
     * @param newShares a map of users to their new share percentages (0.0 to 1.0).
     * Must not be null.
     *
     * @throws IllegalArgumentException if the sum of percentages is not 1.0 or
     * if any percentage is invalid.
     */
    void updateShares(InvoiceItem item, Map<User, BigDecimal> newShares);

    /**
     * Removes all shares from an invoice item, making it unshared.
     *
     * @param item the invoice item to remove shares from. Must not be null.
     */
    void removeShares(InvoiceItem item);

    /**
     * Gets all shares for a specific user in a given month.
     *
     * @param user the user to get shares for. Must not be null.
     *
     * @param month the month to get shares for. Must not be null.
     *
     * @return a list of item shares for the user in the month. Never null, may be empty.
     */
    List<ItemShare> getSharesForUser(User user, YearMonth month);

    /**
     * Gets all shares for a specific invoice item.
     *
     * @param item the invoice item to get shares for. Must not be null.
     *
     * @return a list of shares for the item. Never null, may be empty.
     */
    List<ItemShare> getSharesForItem(InvoiceItem item);

    /**
     * Validates that the sum of share percentages equals 1.0 (100%).
     *
     * @param shares a map of users to their share percentages. Must not be null.
     *
     * @return true if the shares are valid, false otherwise.
     */
    boolean validateShares(Map<User, BigDecimal> shares);

    /**
     * Calculates the amount each user should pay based on their share percentage.
     *
     * @param item the invoice item to calculate amounts for. Must not be null.
     *
     * @param shares a map of users to their share percentages. Must not be null.
     *
     * @return a map of users to their calculated amounts. Never null.
     */
    Map<User, BigDecimal> calculateShareAmounts(InvoiceItem item, Map<User, BigDecimal> shares);
}