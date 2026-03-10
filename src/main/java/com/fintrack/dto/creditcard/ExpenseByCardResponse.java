package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseByCardResponse(
    Long cardId,
    String cardName,
    String lastFourDigits,
    String bankName,
    BigDecimal totalAmount,
    BigDecimal percentage,
    int transactionCount,
    List<CategoryExpenseSummary> categories
) {}
