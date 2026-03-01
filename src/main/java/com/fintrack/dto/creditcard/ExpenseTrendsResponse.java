package com.fintrack.dto.creditcard;

import com.fintrack.dto.user.UserResponse;
import java.util.List;

/**
 * DTO representing expense trends across multiple months.
 *
 * @param user the user this report belongs to
 * @param months the monthly expense data ordered chronologically
 */
public record ExpenseTrendsResponse(
    UserResponse user,
    List<MonthlyExpenseResponse> months
) {}
