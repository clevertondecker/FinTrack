package com.fintrack.dto.creditcard;

import com.fintrack.dto.user.UserResponse;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * DTO representing a complete expense report for a user.
 *
 * @param user the user this report belongs to
 * @param month the month this report covers
 * @param expensesByCategory the list of expenses grouped by category
 * @param totalAmount the total amount of expenses across all categories
 */
public record ExpenseReportResponse(
    UserResponse user,
    YearMonth month,
    List<ExpenseByCategoryResponse> expensesByCategory,
    BigDecimal totalAmount
) {}

