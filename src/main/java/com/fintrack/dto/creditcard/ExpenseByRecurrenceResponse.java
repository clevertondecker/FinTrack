package com.fintrack.dto.creditcard;

import java.math.BigDecimal;

public record ExpenseByRecurrenceResponse(
    String type,
    BigDecimal amount,
    BigDecimal percentage,
    int transactionCount
) {}
