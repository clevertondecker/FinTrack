package com.fintrack.controller.dashboard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fintrack.application.dashboard.DashboardService;
import com.fintrack.application.user.UserService;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.domain.creditcard.InvoiceStatus;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.dashboard.CategoryRankingResponse;
import com.fintrack.dto.dashboard.CreditCardOverviewResponse;
import com.fintrack.dto.dashboard.DailyExpenseResponse;
import com.fintrack.dto.dashboard.DashboardOverviewResponse;
import com.fintrack.dto.user.UserResponse;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("DashboardController Tests")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("John", "john@test.com", "pass123", Set.of(Role.USER));
    }

    @Nested
    @DisplayName("GET /api/dashboard/overview")
    class GetOverviewTests {

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should return dashboard overview for specified month")
        void shouldReturnOverviewForMonth() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);

            YearMonth month = YearMonth.of(2026, 3);
            UserResponse userResp = new UserResponse(
                    1L, "John", "john@test.com",
                    new String[]{"USER"}, LocalDateTime.now(), LocalDateTime.now());

            CreditCardOverviewResponse card = new CreditCardOverviewResponse(
                    1L, "Nubank", "1234", "Nubank", "NU",
                    LocalDate.of(2026, 3, 15),
                    new BigDecimal("2500.00"), new BigDecimal("0"),
                    InvoiceStatus.OPEN);

            CategoryRankingResponse ranking = new CategoryRankingResponse(
                    1L, "Food", "#FF0000",
                    new BigDecimal("1200.00"), new BigDecimal("60.0"), 15);

            DailyExpenseResponse daily = new DailyExpenseResponse(
                    LocalDate.of(2026, 3, 1), new BigDecimal("100.00"));

            DashboardOverviewResponse overview = new DashboardOverviewResponse(
                    userResp, month,
                    new BigDecimal("2000.00"), new BigDecimal("2500.00"),
                    25,
                    List.of(card),
                    List.of(ranking),
                    List.of(daily));

            when(dashboardService.getOverview(eq(testUser), eq(month))).thenReturn(overview);

            mockMvc.perform(get("/api/dashboard/overview")
                    .param("month", "2026-03")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.month").value("2026-03"))
                    .andExpect(jsonPath("$.totalExpenses").value(2000.00))
                    .andExpect(jsonPath("$.totalTransactions").value(25))
                    .andExpect(jsonPath("$.creditCards[0].cardName").value("Nubank"))
                    .andExpect(jsonPath("$.creditCards[0].lastFourDigits").value("1234"))
                    .andExpect(jsonPath("$.categoryRanking[0].categoryName").value("Food"))
                    .andExpect(jsonPath("$.dailyExpenses[0].amount").value(100.00));
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should default to current month when not provided")
        void shouldDefaultToCurrentMonth() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);

            UserResponse userResp = new UserResponse(
                    1L, "John", "john@test.com",
                    new String[]{"USER"}, LocalDateTime.now(), LocalDateTime.now());

            DashboardOverviewResponse overview = new DashboardOverviewResponse(
                    userResp, YearMonth.now(),
                    BigDecimal.ZERO, BigDecimal.ZERO, 0,
                    List.of(), List.of(), List.of());

            when(dashboardService.getOverview(eq(testUser), any(YearMonth.class)))
                    .thenReturn(overview);

            mockMvc.perform(get("/api/dashboard/overview")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTransactions").value(0));
        }
    }
}
