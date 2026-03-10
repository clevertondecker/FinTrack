package com.fintrack.application.dashboard;

import com.fintrack.domain.user.User;
import com.fintrack.dto.dashboard.DashboardOverviewResponse;

import java.time.YearMonth;

public interface DashboardService {
    DashboardOverviewResponse getOverview(User user, YearMonth month);
}
