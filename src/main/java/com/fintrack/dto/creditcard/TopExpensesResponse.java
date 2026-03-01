package com.fintrack.dto.creditcard;

import com.fintrack.dto.user.UserResponse;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * DTO representing the top expenses for a given month.
 *
 * @param user the user this report belongs to
 * @param month the month this report covers
 * @param totalAmount the total expense amount for the month
 * @param topExpenses the list of top expense items ranked by amount
 */
public record TopExpensesResponse(
    UserResponse user,
    YearMonth month,
    BigDecimal totalAmount,
    List<TopExpenseItemResponse> topExpenses
) {}
