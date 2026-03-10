package com.fintrack.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyExpenseResponse(
    LocalDate date,
    BigDecimal amount
) {}
