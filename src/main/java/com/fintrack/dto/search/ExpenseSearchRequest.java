package com.fintrack.dto.search;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseSearchRequest(
    String query,
    Long categoryId,
    Long cardId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
    BigDecimal amountMin,
    BigDecimal amountMax,
    int page,
    int size
) {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    public ExpenseSearchRequest {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            size = DEFAULT_PAGE_SIZE;
        }
    }
}
