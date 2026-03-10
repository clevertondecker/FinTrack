package com.fintrack.dto.budget;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

public record CreateBudgetRequest(
    Long categoryId,
    @NotNull @Positive BigDecimal limitAmount,
    YearMonth month
) {}
