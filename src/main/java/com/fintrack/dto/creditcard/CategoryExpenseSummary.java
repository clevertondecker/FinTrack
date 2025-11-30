package com.fintrack.dto.creditcard;

import java.math.BigDecimal;

/**
 * DTO representing a summary of expenses for a specific category.
 *
 * @param categoryId the category's unique identifier
 * @param categoryName the category's name
 * @param categoryColor the category's color in hex format
 * @param totalAmount the total amount spent in this category
 * @param transactionCount the number of transactions in this category
 */
public record CategoryExpenseSummary(
    Long categoryId,
    String categoryName,
    String categoryColor,
    BigDecimal totalAmount,
    Integer transactionCount
) {
    /**
     * Creates a CategoryExpenseSummary from category and aggregated data.
     *
     * @param categoryId the category ID
     * @param categoryName the category name
     * @param categoryColor the category color
     * @param totalAmount the total amount
     * @param transactionCount the transaction count
     * @return a new CategoryExpenseSummary instance
     */
    public static CategoryExpenseSummary of(
            final Long categoryId,
            final String categoryName,
            final String categoryColor,
            final BigDecimal totalAmount,
            final Integer transactionCount) {
        return new CategoryExpenseSummary(categoryId, categoryName, categoryColor, totalAmount, transactionCount);
    }
}

