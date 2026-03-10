package com.fintrack.controller.dashboard;

import com.fintrack.application.user.UserService;
import com.fintrack.application.dashboard.DashboardService;
import com.fintrack.domain.user.User;
import com.fintrack.dto.dashboard.DashboardOverviewResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(final DashboardService dashboardService,
                               final UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.getCurrentUser(userDetails.getUsername());

        if (month == null) {
            month = YearMonth.now();
        }

        return ResponseEntity.ok(dashboardService.getOverview(user, month));
    }
}
