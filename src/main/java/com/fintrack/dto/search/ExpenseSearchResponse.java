package com.fintrack.dto.search;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseSearchResponse(
    List<ExpenseSearchResult> results,
    int totalResults,
    int page,
    int totalPages,
    BigDecimal totalAmount
) {}
