package com.fintrack.dto.search;

import com.fintrack.dto.creditcard.CategoryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record ExpenseSearchResult(
    Long itemId,
    Long invoiceId,
    String description,
    BigDecimal amount,
    LocalDate purchaseDate,
    CategoryResponse category,
    String cardName,
    String lastFourDigits,
    YearMonth invoiceMonth,
    int installments,
    int totalInstallments
) {}
