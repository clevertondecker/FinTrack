package com.fintrack.controller.creditcard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fintrack.application.creditcard.ExpenseReportAssemblyService;
import com.fintrack.application.user.UserService;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.CategoryResponse;
import com.fintrack.dto.creditcard.ExpenseByCategoryResponse;
import com.fintrack.dto.creditcard.ExpenseDetailResponse;
import com.fintrack.dto.creditcard.ExpenseReportResponse;
import com.fintrack.dto.creditcard.CategoryExpenseSummary;
import com.fintrack.dto.dashboard.DailyExpenseResponse;
import com.fintrack.dto.user.UserResponse;

/**
 * Unit tests for ExpenseReportController.
 * Tests the REST endpoints for expense reports by category.
 */
@WebMvcTest(ExpenseReportController.class)
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("ExpenseReportController Tests")
class ExpenseReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseReportAssemblyService assemblyService;

    @MockBean
    private UserService userService;

    private User testUser;
    private UserResponse testUserResponse;
    private Category foodCategory;
    private Category transportCategory;
    private YearMonth testMonth;

    @BeforeEach
    void setUp() throws Exception {
        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testUserResponse = new UserResponse(null, "John Doe", "john@example.com",
                new String[]{"USER"}, null, null);

        foodCategory = Category.of("Food", "#FF0000");
        transportCategory = Category.of("Transport", "#0000FF");

        java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
        foodIdField.setAccessible(true);
        foodIdField.set(foodCategory, 1L);

        java.lang.reflect.Field transportIdField = Category.class.getDeclaredField("id");
        transportIdField.setAccessible(true);
        transportIdField.set(transportCategory, 2L);

        testMonth = YearMonth.of(2024, 11);
    }

    @Nested
    @DisplayName("GET /api/expenses/by-category Tests")
    class GetExpensesByCategoryTests {

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return expense report successfully")
        void shouldReturnExpenseReportSuccessfully() throws Exception {
            List<ExpenseDetailResponse> foodDetails = List.of(
                    new ExpenseDetailResponse(null, 1L, "Groceries", new BigDecimal("100.00"),
                            LocalDate.of(2024, 10, 15), 1L)
            );
            List<ExpenseDetailResponse> transportDetails = List.of(
                    new ExpenseDetailResponse(null, 2L, "Uber", new BigDecimal("50.00"),
                            LocalDate.of(2024, 10, 20), 1L)
            );

            List<DailyExpenseResponse> foodDaily = List.of(
                    new DailyExpenseResponse(LocalDate.of(2024, 10, 15), new BigDecimal("100.00")));
            List<DailyExpenseResponse> transportDaily = List.of(
                    new DailyExpenseResponse(LocalDate.of(2024, 10, 20), new BigDecimal("50.00")));

            ExpenseReportResponse report = new ExpenseReportResponse(
                    testUserResponse, testMonth,
                    List.of(
                            new ExpenseByCategoryResponse(
                                    CategoryResponse.from(foodCategory), new BigDecimal("100.00"),
                                    new BigDecimal("66.7"), 1, foodDetails, foodDaily),
                            new ExpenseByCategoryResponse(
                                    CategoryResponse.from(transportCategory), new BigDecimal("50.00"),
                                    new BigDecimal("33.3"), 1, transportDetails, transportDaily)
                    ),
                    new BigDecimal("150.00")
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildCategoryReport(testUser, testMonth, false)).thenReturn(report);

            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.month").value("2024-11"))
                    .andExpect(jsonPath("$.totalAmount").value(150.00))
                    .andExpect(jsonPath("$.expensesByCategory").isArray())
                    .andExpect(jsonPath("$.expensesByCategory.length()").value(2))
                    .andExpect(jsonPath("$.expensesByCategory[0].totalAmount").exists())
                    .andExpect(jsonPath("$.expensesByCategory[0].details").isArray());
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should default to current month when month not provided")
        void shouldDefaultToCurrentMonthWhenMonthNotProvided() throws Exception {
            YearMonth currentMonth = YearMonth.now();

            ExpenseReportResponse report = new ExpenseReportResponse(
                    testUserResponse, currentMonth,
                    List.of(
                            ExpenseByCategoryResponse.withoutDetails(
                                    CategoryResponse.from(foodCategory), new BigDecimal("100.00"),
                                    new BigDecimal("100.0"), 1)
                    ),
                    new BigDecimal("100.00")
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildCategoryReport(testUser, currentMonth, false)).thenReturn(report);

            mockMvc.perform(get("/api/expenses/by-category")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.month").value(currentMonth.toString()))
                    .andExpect(jsonPath("$.totalAmount").value(100.00));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should filter by category ID when provided")
        void shouldFilterByCategoryIdWhenProvided() throws Exception {
            List<ExpenseDetailResponse> foodDetails = List.of(
                    new ExpenseDetailResponse(null, 1L, "Groceries", new BigDecimal("100.00"),
                            LocalDate.of(2024, 10, 15), 1L)
            );

            List<DailyExpenseResponse> foodDaily = List.of(
                    new DailyExpenseResponse(LocalDate.of(2024, 10, 15), new BigDecimal("100.00")));

            ExpenseReportResponse report = new ExpenseReportResponse(
                    testUserResponse, testMonth,
                    List.of(
                            new ExpenseByCategoryResponse(
                                    CategoryResponse.from(foodCategory), new BigDecimal("100.00"),
                                    new BigDecimal("66.7"), 1, foodDetails, foodDaily)
                    ),
                    new BigDecimal("150.00")
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildFilteredCategoryReport(testUser, testMonth, 1L, false))
                    .thenReturn(report);

            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .param("categoryId", "1")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expensesByCategory.length()").value(1))
                    .andExpect(jsonPath("$.expensesByCategory[0].category.name").value("Food"));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return empty report when category ID not found")
        void shouldReturnEmptyReportWhenCategoryIdNotFound() throws Exception {
            ExpenseReportResponse emptyReport = new ExpenseReportResponse(
                    testUserResponse, testMonth, List.of(), BigDecimal.ZERO);

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildFilteredCategoryReport(testUser, testMonth, 999L, false))
                    .thenReturn(emptyReport);

            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .param("categoryId", "999")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expensesByCategory").isArray())
                    .andExpect(jsonPath("$.expensesByCategory.length()").value(0))
                    .andExpect(jsonPath("$.totalAmount").value(0));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return 400 when user not found")
        void shouldReturn400WhenUserNotFound() throws Exception {
            when(userService.getCurrentUser("john@example.com"))
                    .thenThrow(new IllegalArgumentException("User not found"));

            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return empty report when no expenses exist")
        void shouldReturnEmptyReportWhenNoExpensesExist() throws Exception {
            ExpenseReportResponse emptyReport = new ExpenseReportResponse(
                    testUserResponse, testMonth, List.of(), BigDecimal.ZERO);

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildCategoryReport(testUser, testMonth, false))
                    .thenReturn(emptyReport);

            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expensesByCategory").isArray())
                    .andExpect(jsonPath("$.expensesByCategory.length()").value(0))
                    .andExpect(jsonPath("$.totalAmount").value(0));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should sort categories by total amount descending")
        void shouldSortCategoriesByTotalAmountDescending() throws Exception {
            ExpenseReportResponse report = new ExpenseReportResponse(
                    testUserResponse, testMonth,
                    List.of(
                            ExpenseByCategoryResponse.withoutDetails(
                                    CategoryResponse.from(transportCategory), new BigDecimal("100.00"),
                                    new BigDecimal("66.7"), 0),
                            ExpenseByCategoryResponse.withoutDetails(
                                    CategoryResponse.from(foodCategory), new BigDecimal("50.00"),
                                    new BigDecimal("33.3"), 0)
                    ),
                    new BigDecimal("150.00")
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildCategoryReport(testUser, testMonth, false)).thenReturn(report);

            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expensesByCategory[0].totalAmount").value(100.00))
                    .andExpect(jsonPath("$.expensesByCategory[1].totalAmount").value(50.00));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return total expenses when showTotal is true")
        void shouldReturnTotalExpensesWhenShowTotalIsTrue() throws Exception {
            List<ExpenseDetailResponse> totalDetails = List.of(
                    new ExpenseDetailResponse(null, 1L, "Groceries", new BigDecimal("200.00"),
                            LocalDate.of(2024, 10, 15), 1L)
            );

            List<DailyExpenseResponse> dailyBreakdown = List.of(
                    new DailyExpenseResponse(LocalDate.of(2024, 10, 15), new BigDecimal("200.00")));

            ExpenseReportResponse report = new ExpenseReportResponse(
                    testUserResponse, testMonth,
                    List.of(
                            new ExpenseByCategoryResponse(
                                    CategoryResponse.from(foodCategory), new BigDecimal("200.00"),
                                    new BigDecimal("100.0"), 1, totalDetails, dailyBreakdown)
                    ),
                    new BigDecimal("200.00")
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildCategoryReport(testUser, testMonth, true)).thenReturn(report);

            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .param("showTotal", "true")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAmount").value(200.00))
                    .andExpect(jsonPath("$.expensesByCategory[0].totalAmount").value(200.00));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return user expenses when showTotal is false")
        void shouldReturnUserExpensesWhenShowTotalIsFalse() throws Exception {
            List<ExpenseDetailResponse> userDetails = List.of(
                    new ExpenseDetailResponse(1L, 1L, "Groceries", new BigDecimal("100.00"),
                            LocalDate.of(2024, 10, 15), 1L)
            );

            List<DailyExpenseResponse> dailyBreakdown = List.of(
                    new DailyExpenseResponse(LocalDate.of(2024, 10, 15), new BigDecimal("100.00")));

            ExpenseReportResponse report = new ExpenseReportResponse(
                    testUserResponse, testMonth,
                    List.of(
                            new ExpenseByCategoryResponse(
                                    CategoryResponse.from(foodCategory), new BigDecimal("100.00"),
                                    new BigDecimal("100.0"), 1, userDetails, dailyBreakdown)
                    ),
                    new BigDecimal("100.00")
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildCategoryReport(testUser, testMonth, false)).thenReturn(report);

            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .param("showTotal", "false")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAmount").value(100.00))
                    .andExpect(jsonPath("$.expensesByCategory[0].totalAmount").value(100.00));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should default showTotal to false when not provided")
        void shouldDefaultShowTotalToFalseWhenNotProvided() throws Exception {
            ExpenseReportResponse report = new ExpenseReportResponse(
                    testUserResponse, testMonth,
                    List.of(
                            ExpenseByCategoryResponse.withoutDetails(
                                    CategoryResponse.from(foodCategory), new BigDecimal("100.00"),
                                    new BigDecimal("100.0"), 1)
                    ),
                    new BigDecimal("100.00")
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildCategoryReport(testUser, testMonth, false)).thenReturn(report);

            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAmount").value(100.00));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/summary Tests")
    class GetExpenseSummaryTests {

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return expense summary successfully")
        void shouldReturnExpenseSummarySuccessfully() throws Exception {
            Map<String, Object> summary = Map.of(
                    "user", testUserResponse,
                    "month", "2024-11",
                    "totalAmount", new BigDecimal("150.00"),
                    "expensesByCategory", List.of(
                            new CategoryExpenseSummary(1L, "Food", "#FF0000", new BigDecimal("100.00"), 1),
                            new CategoryExpenseSummary(2L, "Transport", "#0000FF", new BigDecimal("50.00"), 1)
                    )
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildSummary(testUser, testMonth)).thenReturn(summary);

            mockMvc.perform(get("/api/expenses/summary")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.month").value("2024-11"))
                    .andExpect(jsonPath("$.totalAmount").value(150.00))
                    .andExpect(jsonPath("$.expensesByCategory").isArray())
                    .andExpect(jsonPath("$.expensesByCategory.length()").value(2));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should default to current month when month not provided")
        void shouldDefaultToCurrentMonthWhenMonthNotProvided() throws Exception {
            YearMonth currentMonth = YearMonth.now();

            Map<String, Object> summary = Map.of(
                    "user", testUserResponse,
                    "month", currentMonth.toString(),
                    "totalAmount", BigDecimal.ZERO,
                    "expensesByCategory", List.of()
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildSummary(testUser, currentMonth)).thenReturn(summary);

            mockMvc.perform(get("/api/expenses/summary")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.month").value(currentMonth.toString()));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return 400 when user not found")
        void shouldReturn400WhenUserNotFound() throws Exception {
            when(userService.getCurrentUser("john@example.com"))
                    .thenThrow(new IllegalArgumentException("User not found"));

            mockMvc.perform(get("/api/expenses/summary")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should include transaction count in summary")
        void shouldIncludeTransactionCountInSummary() throws Exception {
            Map<String, Object> summary = Map.of(
                    "user", testUserResponse,
                    "month", "2024-11",
                    "totalAmount", new BigDecimal("100.00"),
                    "expensesByCategory", List.of(
                            new CategoryExpenseSummary(1L, "Food", "#FF0000", new BigDecimal("100.00"), 2)
                    )
            );

            when(userService.getCurrentUser("john@example.com")).thenReturn(testUser);
            when(assemblyService.buildSummary(testUser, testMonth)).thenReturn(summary);

            mockMvc.perform(get("/api/expenses/summary")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expensesByCategory[0].transactionCount").value(2));
        }
    }
}
