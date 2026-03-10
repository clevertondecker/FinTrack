package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record PeriodComparisonResponse(
    MonthSummary current,
    MonthSummary comparison,
    BigDecimal differenceAmount,
    BigDecimal differencePercentage,
    List<CategoryComparison> categoryComparisons
) {

    public record MonthSummary(
        YearMonth month,
        BigDecimal totalAmount,
        int transactionCount
    ) {}

    public record CategoryComparison(
        CategoryResponse category,
        BigDecimal currentAmount,
        BigDecimal comparisonAmount,
        BigDecimal differenceAmount,
        BigDecimal differencePercentage
    ) {}
}
