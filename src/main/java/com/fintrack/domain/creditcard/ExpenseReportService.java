package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import java.math.BigDecimal;
import java.time.YearMonth;
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
     * Includes both shared items (via ItemShare) and unshared items (for card owner).
     *
     * @param user the user to generate the report for. Must not be null.
     * @param month the month to generate the report for. Must not be null.
     * @return a map of categories to their total amounts. Never null, may be empty.
     *         Categories without expenses are not included in the map.
     */
    Map<Category, BigDecimal> getExpensesByCategory(User user, YearMonth month);

    /**
     * Gets the total amount of expenses for a user in a given month across all categories.
     *
     * @param user the user to calculate total expenses for. Must not be null.
     * @param month the month to calculate expenses for. Must not be null.
     * @return the total amount of expenses. Never null.
     */
    BigDecimal getTotalExpenses(User user, YearMonth month);
}

