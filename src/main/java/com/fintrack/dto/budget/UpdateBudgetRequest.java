package com.fintrack.dto.budget;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateBudgetRequest(
    @NotNull @Positive BigDecimal limitAmount
) {}
