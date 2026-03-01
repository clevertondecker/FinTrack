package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.ExpenseDetailResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Domain service for generating expense reports by category.
 * Provides business logic for aggregating user expenses grouped by category.
 */
public interface ExpenseReportService {

    /**
     * Calculates the total amount a user spent in a specific category for a given month.
     *
     * @param user the user to calculate expenses for. Must not be null.
     * @param month the month to calculate expenses for. Must not be null.
     * @param category the category to filter by. Must not be null.
     * @return the total amount spent in the category. Never null.
     */
    BigDecimal getTotalByCategory(User user, YearMonth month, Category category);

    /**
     * Gets a summary of expenses grouped by category for a user in a given month.
     * Includes both shared items (via ItemShare) and unshared items (for a card owner).
     *
     * @param user the user to generate the report for. Must not be null.
     * @param month the month to generate the report for. Must not be null.
     * @return a map of categories to their total amounts. Never null, may be empty.
     *         Categories without expenses are not included in the map.
     */
    Map<Category, BigDecimal> getExpensesByCategory(User user, YearMonth month);

    /**
     * Gets the total number of expenses for a user in a given month across all categories.
     *
     * @param user the user to calculate total expenses for. Must not be null.
     * @param month the month to calculate expenses for. Must not be null.
     * @return the total number of expenses. Never null.
     */
    BigDecimal getTotalExpenses(User user, YearMonth month);

    /**
     * Gets total expenses by category for cards owned by the given user.
     *
     * @param user the card owner. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @return a map of category to total expense amount. Never null.
     */
    Map<Category, BigDecimal> getTotalExpensesByCategory(User user, YearMonth month);

    /**
     * Gets grand total expenses for all items on cards owned by the given user.
     *
     * @param user the card owner. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @return the total expense amount. Never null.
     */
    BigDecimal getGrandTotalExpenses(User user, YearMonth month);

    /**
     * Gets detailed expense information for a specific user, month, and category.
     *
     * @param user the user to get expenses for. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @param category the category to filter by. Can be null for uncategorized items.
     * @return a list of detailed expense entries. Never null.
     */
    List<ExpenseDetailResponse> getExpenseDetails(User user, YearMonth month, Category category);

    /**
     * Gets detailed total expense information for cards owned by the given user.
     *
     * @param user the card owner. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @param category the category to filter by. Can be null for uncategorized items.
     * @return a list of detailed expense entries. Never null.
     */
    List<ExpenseDetailResponse> getTotalExpenseDetails(User user, YearMonth month, Category category);

    /**
     * Gets expenses grouped by month and category for a given date range.
     * Used for trend analysis and month-over-month comparison.
     *
     * @param user the user to generate the report for. Must not be null.
     * @param from the start month (inclusive). Must not be null.
     * @param to the end month (inclusive). Must not be null.
     * @return a map of months to their category-amount breakdowns. Never null.
     */
    Map<YearMonth, Map<Category, BigDecimal>> getExpensesByMonthAndCategory(
        User user, YearMonth from, YearMonth to);

    /**
     * Gets total expenses (card owner view) grouped by month and category.
     *
     * @param user the card owner. Must not be null.
     * @param from the start month (inclusive). Must not be null.
     * @param to the end month (inclusive). Must not be null.
     * @return a map of months to category-amount breakdowns. Never null.
     */
    Map<YearMonth, Map<Category, BigDecimal>> getTotalExpensesByMonthAndCategory(
        User user, YearMonth from, YearMonth to);

    /**
     * Gets the top expenses for a user in a given month, sorted by amount descending.
     *
     * @param user the user to get top expenses for. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @param limit the maximum number of items to return. Must be positive.
     * @return a list of expense items sorted by amount descending. Never null.
     */
    List<TopExpenseEntry> getTopExpenses(User user, YearMonth month, int limit);

    /**
     * Gets top expenses for card owner view (total amounts, not shares).
     *
     * @param user the card owner. Must not be null.
     * @param month the month to get expenses for. Must not be null.
     * @param limit the maximum number of items to return. Must be positive.
     * @return a list of top expense entries sorted by amount descending. Never null.
     */
    List<TopExpenseEntry> getTotalTopExpenses(User user, YearMonth month, int limit);

    /**
     * Represents a top expense entry with item details and user share amount.
     *
     * @param itemId the invoice item ID
     * @param description the item description
     * @param amount the user's share amount
     * @param purchaseDate the purchase date
     * @param invoiceId the invoice ID
     * @param category the item category (null if uncategorized)
     */
    record TopExpenseEntry(
        Long itemId,
        String description,
        BigDecimal amount,
        LocalDate purchaseDate,
        Long invoiceId,
        Category category
    ) {}
}

