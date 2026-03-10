package com.fintrack.dto.budget;

import com.fintrack.dto.creditcard.CategoryResponse;

import java.math.BigDecimal;

public record BudgetStatusResponse(
    Long budgetId,
    CategoryResponse category,
    BigDecimal budgetLimit,
    BigDecimal actualSpent,
    BigDecimal remaining,
    BigDecimal utilizationPercent,
    String status
) {}
