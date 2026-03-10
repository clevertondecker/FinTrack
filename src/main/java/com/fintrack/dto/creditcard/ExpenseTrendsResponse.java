package com.fintrack.dto.creditcard;

import com.fintrack.dto.user.UserResponse;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseTrendsResponse(
    UserResponse user,
    List<MonthlyExpenseResponse> months,
    BigDecimal averageMonthly,
    BigDecimal currentVsAveragePercent,
    BigDecimal currentVsPreviousMonthPercent,
    BigDecimal highestMonthAmount,
    BigDecimal lowestMonthAmount
) {
    public ExpenseTrendsResponse(final UserResponse user,
                                 final List<MonthlyExpenseResponse> months) {
        this(user, months, null, null, null, null, null);
    }
}
