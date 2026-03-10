package com.fintrack.dto.budget;

import com.fintrack.dto.creditcard.CategoryResponse;

import java.math.BigDecimal;
import java.time.YearMonth;

public record BudgetResponse(
    Long id,
    CategoryResponse category,
    BigDecimal limitAmount,
    YearMonth month,
    boolean recurring,
    boolean active
) {}
