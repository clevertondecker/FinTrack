package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing expenses grouped by a specific category.
 *
 * @param category the category information
 * @param totalAmount the total amount spent in this category
 * @param transactionCount the number of transactions in this category
 * @param details the list of detailed expense entries (optional, may be empty)
 */
public record ExpenseByCategoryResponse(
    CategoryResponse category,
    BigDecimal totalAmount,
    Integer transactionCount,
    List<ExpenseDetailResponse> details
) {
    /**
     * Creates an ExpenseByCategoryResponse without details.
     *
     * @param category the category
     * @param totalAmount the total amount
     * @param transactionCount the transaction count
     * @return a new ExpenseByCategoryResponse with empty details
     */
    public static ExpenseByCategoryResponse withoutDetails(
            final CategoryResponse category,
            final BigDecimal totalAmount,
            final Integer transactionCount) {
        return new ExpenseByCategoryResponse(category, totalAmount, transactionCount, List.of());
    }
}

