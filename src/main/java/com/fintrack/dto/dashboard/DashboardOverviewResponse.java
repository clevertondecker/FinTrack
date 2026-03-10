package com.fintrack.dto.dashboard;

import com.fintrack.dto.user.UserResponse;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record DashboardOverviewResponse(
    UserResponse user,
    YearMonth month,
    BigDecimal totalExpenses,
    BigDecimal totalExpensesGross,
    int totalTransactions,
    List<CreditCardOverviewResponse> creditCards,
    List<CategoryRankingResponse> categoryRanking,
    List<DailyExpenseResponse> dailyExpenses
) {}
