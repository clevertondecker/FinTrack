package com.fintrack.controller.budget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.fintrack.application.budget.BudgetService;
import com.fintrack.application.user.UserService;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.budget.BudgetResponse;
import com.fintrack.dto.budget.BudgetStatusResponse;
import com.fintrack.dto.creditcard.CategoryResponse;

@WebMvcTest(BudgetController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("BudgetController Tests")
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BudgetService budgetService;

    @MockBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("John", "john@test.com", "pass123", Set.of(Role.USER));
    }

    @Nested
    @DisplayName("POST /api/budgets")
    class CreateBudgetTests {

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should create budget successfully")
        void shouldCreateBudgetSuccessfully() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);

            CategoryResponse catResp = new CategoryResponse(1L, "Food", "#FF0000", null, null);
            BudgetResponse response = new BudgetResponse(
                    10L, catResp, new BigDecimal("1000.00"),
                    YearMonth.of(2026, 3), false, true);

            when(budgetService.create(eq(testUser), any())).thenReturn(response);

            mockMvc.perform(post("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "categoryId": 1,
                          "limitAmount": 1000.00,
                          "month": "2026-03"
                        }
                        """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.category.name").value("Food"))
                    .andExpect(jsonPath("$.limitAmount").value(1000.00))
                    .andExpect(jsonPath("$.recurring").value(false))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should return 400 when limit is missing")
        void shouldReturn400WhenLimitMissing() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);

            mockMvc.perform(post("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "categoryId": 1
                        }
                        """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/budgets")
    class GetBudgetsTests {

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should return budget statuses for month")
        void shouldReturnBudgetStatusesForMonth() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);

            CategoryResponse catResp = new CategoryResponse(1L, "Food", "#FF0000", null, null);
            BudgetStatusResponse statusResponse = new BudgetStatusResponse(
                    1L, catResp, new BigDecimal("1000.00"),
                    new BigDecimal("500.00"), new BigDecimal("500.00"),
                    new BigDecimal("50.0"), "UNDER_BUDGET");

            when(budgetService.getStatusForMonth(eq(testUser), eq(YearMonth.of(2026, 3))))
                    .thenReturn(List.of(statusResponse));

            mockMvc.perform(get("/api/budgets")
                    .param("month", "2026-03")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].budgetId").value(1))
                    .andExpect(jsonPath("$[0].budgetLimit").value(1000.00))
                    .andExpect(jsonPath("$[0].actualSpent").value(500.00))
                    .andExpect(jsonPath("$[0].remaining").value(500.00))
                    .andExpect(jsonPath("$[0].utilizationPercent").value(50.0))
                    .andExpect(jsonPath("$[0].status").value("UNDER_BUDGET"));
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should default to current month when month not provided")
        void shouldDefaultToCurrentMonth() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);
            when(budgetService.getStatusForMonth(eq(testUser), any(YearMonth.class)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("PUT /api/budgets/{id}")
    class UpdateBudgetTests {

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should update budget successfully")
        void shouldUpdateBudgetSuccessfully() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);

            CategoryResponse catResp = new CategoryResponse(1L, "Food", "#FF0000", null, null);
            BudgetResponse response = new BudgetResponse(
                    10L, catResp, new BigDecimal("1500.00"),
                    YearMonth.of(2026, 3), false, true);

            when(budgetService.update(eq(testUser), eq(10L), any())).thenReturn(response);

            mockMvc.perform(put("/api/budgets/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "limitAmount": 1500.00
                        }
                        """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.limitAmount").value(1500.00));
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should return 400 when budget not found")
        void shouldReturn400WhenBudgetNotFound() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);
            when(budgetService.update(eq(testUser), eq(999L), any()))
                    .thenThrow(new IllegalArgumentException("Budget not found: 999"));

            mockMvc.perform(put("/api/budgets/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "limitAmount": 1500.00
                        }
                        """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/budgets/{id}")
    class DeleteBudgetTests {

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should delete budget successfully")
        void shouldDeleteBudgetSuccessfully() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);
            doNothing().when(budgetService).delete(testUser, 10L);

            mockMvc.perform(delete("/api/budgets/10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should return 400 when budget not found for delete")
        void shouldReturn400WhenBudgetNotFoundForDelete() throws Exception {
            when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);
            doThrow(new IllegalArgumentException("Budget not found: 999"))
                    .when(budgetService).delete(testUser, 999L);

            mockMvc.perform(delete("/api/budgets/999")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }
}
