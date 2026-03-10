package com.fintrack.dto.creditcard;

import com.fintrack.dto.dashboard.DailyExpenseResponse;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseByCategoryResponse(
    CategoryResponse category,
    BigDecimal totalAmount,
    BigDecimal percentage,
    Integer transactionCount,
    List<ExpenseDetailResponse> details,
    List<DailyExpenseResponse> dailyBreakdown
) {
    public static ExpenseByCategoryResponse withoutDetails(
            final CategoryResponse category,
            final BigDecimal totalAmount,
            final BigDecimal percentage,
            final Integer transactionCount) {
        return new ExpenseByCategoryResponse(
                category, totalAmount, percentage, transactionCount, List.of(), List.of());
    }
}
