package com.fintrack.dto.dashboard;

import java.math.BigDecimal;

public record CategoryRankingResponse(
    Long categoryId,
    String categoryName,
    String color,
    BigDecimal amount,
    BigDecimal percentage,
    int transactionCount
) {}
