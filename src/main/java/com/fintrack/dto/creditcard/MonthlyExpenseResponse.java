package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * DTO representing expense data for a single month, broken down by category.
 *
 * @param month the month this data covers
 * @param totalAmount the total expense amount for the month
 * @param categories the expense breakdown by category
 */
public record MonthlyExpenseResponse(
    YearMonth month,
    BigDecimal totalAmount,
    List<CategoryExpenseSummary> categories
) {}
