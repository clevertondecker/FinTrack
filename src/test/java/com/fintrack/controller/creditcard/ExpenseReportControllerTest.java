package com.fintrack.controller.creditcard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import com.fintrack.application.creditcard.ExpenseReportServiceImpl;
import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.ExpenseDetailResponse;

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
    private ExpenseReportServiceImpl expenseReportService;

    @MockBean
    private InvoiceService invoiceService;

    private User testUser;
    private Category foodCategory;
    private Category transportCategory;
    private YearMonth testMonth;

    @BeforeEach
    void setUp() throws Exception {
        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        foodCategory = Category.of("Food", "#FF0000");
        transportCategory = Category.of("Transport", "#0000FF");
        
        // Set category IDs for proper comparison in Map
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
            // Given
            Map<Category, BigDecimal> expensesByCategory = Map.of(
                    foodCategory, new BigDecimal("100.00"),
                    transportCategory, new BigDecimal("50.00")
            );

            List<ExpenseDetailResponse> foodDetails = List.of(
                    new ExpenseDetailResponse(null, 1L, "Groceries", new BigDecimal("100.00"), 
                            LocalDate.of(2024, 10, 15), 1L)
            );

            List<ExpenseDetailResponse> transportDetails = List.of(
                    new ExpenseDetailResponse(null, 2L, "Uber", new BigDecimal("50.00"), 
                            LocalDate.of(2024, 10, 20), 1L)
            );

            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.of(testUser));
            when(expenseReportService.getExpensesByCategory(testUser, testMonth))
                    .thenReturn(expensesByCategory);
            when(expenseReportService.getExpenseDetails(testUser, testMonth, foodCategory))
                    .thenReturn(foodDetails);
            when(expenseReportService.getExpenseDetails(testUser, testMonth, transportCategory))
                    .thenReturn(transportDetails);
            when(expenseReportService.getTotalExpenses(testUser, testMonth))
                    .thenReturn(new BigDecimal("150.00"));

            // When & Then
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
            // Given
            YearMonth currentMonth = YearMonth.now();
            Map<Category, BigDecimal> expensesByCategory = Map.of(
                    foodCategory, new BigDecimal("100.00")
            );

            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.of(testUser));
            when(expenseReportService.getExpensesByCategory(testUser, currentMonth))
                    .thenReturn(expensesByCategory);
            when(expenseReportService.getExpenseDetails(any(), any(), any()))
                    .thenReturn(List.of());
            when(expenseReportService.getTotalExpenses(testUser, currentMonth))
                    .thenReturn(new BigDecimal("100.00"));

            // When & Then
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
            // Given
            Map<Category, BigDecimal> allExpenses = Map.of(
                    foodCategory, new BigDecimal("100.00"),
                    transportCategory, new BigDecimal("50.00")
            );

            List<ExpenseDetailResponse> foodDetails = List.of(
                    new ExpenseDetailResponse(null, 1L, "Groceries", new BigDecimal("100.00"), 
                            LocalDate.of(2024, 10, 15), 1L)
            );

            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.of(testUser));
            when(expenseReportService.getExpensesByCategory(testUser, testMonth))
                    .thenReturn(allExpenses);
            when(expenseReportService.getExpenseDetails(testUser, testMonth, foodCategory))
                    .thenReturn(foodDetails);
            when(expenseReportService.getTotalExpenses(testUser, testMonth))
                    .thenReturn(new BigDecimal("150.00"));

            // Set category ID via reflection
            try {
                java.lang.reflect.Field idField = Category.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(foodCategory, 1L);
            } catch (Exception e) {
                // Ignore if reflection fails
            }

            // When & Then
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
            // Given
            Map<Category, BigDecimal> expensesByCategory = Map.of(
                    foodCategory, new BigDecimal("100.00")
            );

            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.of(testUser));
            when(expenseReportService.getExpensesByCategory(testUser, testMonth))
                    .thenReturn(expensesByCategory);

            // When & Then
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
            // Given
            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return empty report when no expenses exist")
        void shouldReturnEmptyReportWhenNoExpensesExist() throws Exception {
            // Given
            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.of(testUser));
            when(expenseReportService.getExpensesByCategory(testUser, testMonth))
                    .thenReturn(Map.of());
            when(expenseReportService.getTotalExpenses(testUser, testMonth))
                    .thenReturn(BigDecimal.ZERO);

            // When & Then
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
            // Given
            Map<Category, BigDecimal> expensesByCategory = Map.of(
                    foodCategory, new BigDecimal("50.00"),
                    transportCategory, new BigDecimal("100.00")
            );

            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.of(testUser));
            when(expenseReportService.getExpensesByCategory(testUser, testMonth))
                    .thenReturn(expensesByCategory);
            when(expenseReportService.getExpenseDetails(any(), any(), any()))
                    .thenReturn(List.of());
            when(expenseReportService.getTotalExpenses(testUser, testMonth))
                    .thenReturn(new BigDecimal("150.00"));

            // When & Then
            mockMvc.perform(get("/api/expenses/by-category")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expensesByCategory[0].totalAmount").value(100.00))
                    .andExpect(jsonPath("$.expensesByCategory[1].totalAmount").value(50.00));
        }
    }

    @Nested
    @DisplayName("GET /api/expenses/summary Tests")
    class GetExpenseSummaryTests {

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return expense summary successfully")
        void shouldReturnExpenseSummarySuccessfully() throws Exception {
            // Given
            Map<Category, BigDecimal> expensesByCategory = Map.of(
                    foodCategory, new BigDecimal("100.00"),
                    transportCategory, new BigDecimal("50.00")
            );

            List<ExpenseDetailResponse> foodDetails = List.of(
                    new ExpenseDetailResponse(null, 1L, "Groceries", new BigDecimal("100.00"), 
                            LocalDate.of(2024, 10, 15), 1L)
            );

            List<ExpenseDetailResponse> transportDetails = List.of(
                    new ExpenseDetailResponse(null, 2L, "Uber", new BigDecimal("50.00"), 
                            LocalDate.of(2024, 10, 20), 1L)
            );

            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.of(testUser));
            when(expenseReportService.getTotalExpenses(testUser, testMonth))
                    .thenReturn(new BigDecimal("150.00"));
            when(expenseReportService.getExpensesByCategory(testUser, testMonth))
                    .thenReturn(expensesByCategory);
            when(expenseReportService.getExpenseDetails(testUser, testMonth, foodCategory))
                    .thenReturn(foodDetails);
            when(expenseReportService.getExpenseDetails(testUser, testMonth, transportCategory))
                    .thenReturn(transportDetails);

            // When & Then
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
            // Given
            YearMonth currentMonth = YearMonth.now();

            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.of(testUser));
            when(expenseReportService.getTotalExpenses(testUser, currentMonth))
                    .thenReturn(BigDecimal.ZERO);
            when(expenseReportService.getExpensesByCategory(testUser, currentMonth))
                    .thenReturn(Map.of());

            // When & Then
            mockMvc.perform(get("/api/expenses/summary")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.month").value(currentMonth.toString()));
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should return 400 when user not found")
        void shouldReturn400WhenUserNotFound() throws Exception {
            // Given
            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/expenses/summary")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "john@example.com")
        @DisplayName("Should include transaction count in summary")
        void shouldIncludeTransactionCountInSummary() throws Exception {
            // Given
            Map<Category, BigDecimal> expensesByCategory = Map.of(
                    foodCategory, new BigDecimal("100.00")
            );

            List<ExpenseDetailResponse> foodDetails = List.of(
                    new ExpenseDetailResponse(null, 1L, "Item 1", new BigDecimal("50.00"), 
                            LocalDate.of(2024, 10, 15), 1L),
                    new ExpenseDetailResponse(null, 2L, "Item 2", new BigDecimal("50.00"), 
                            LocalDate.of(2024, 10, 20), 1L)
            );

            when(invoiceService.findUserByUsername("john@example.com")).thenReturn(Optional.of(testUser));
            when(expenseReportService.getTotalExpenses(testUser, testMonth))
                    .thenReturn(new BigDecimal("100.00"));
            when(expenseReportService.getExpensesByCategory(testUser, testMonth))
                    .thenReturn(expensesByCategory);
            when(expenseReportService.getExpenseDetails(testUser, testMonth, foodCategory))
                    .thenReturn(foodDetails);

            // When & Then
            mockMvc.perform(get("/api/expenses/summary")
                    .param("month", "2024-11")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.expensesByCategory[0].transactionCount").value(2));
        }
    }
}

