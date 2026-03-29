package com.fintrack.controller.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fintrack.application.search.ExpenseSearchService;
import com.fintrack.application.user.UserService;
import com.fintrack.config.TestSecurityConfig;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.CategoryResponse;
import com.fintrack.dto.search.ExpenseSearchResponse;
import com.fintrack.dto.search.ExpenseSearchResult;

@WebMvcTest(ExpenseSearchController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("ExpenseSearchController Tests")
class ExpenseSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseSearchService searchService;

    @MockBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("John", "john@test.com", "pass123", Set.of(Role.USER));
        when(userService.getCurrentUser("john@test.com")).thenReturn(testUser);
    }

    @Nested
    @DisplayName("GET /api/expenses/search")
    class SearchTests {

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should return search results with query parameter")
        void shouldReturnSearchResults() throws Exception {
            CategoryResponse cat = new CategoryResponse(1L, "Food", "#FF0000", null, null);
            ExpenseSearchResult result = new ExpenseSearchResult(
                    10L, 5L, "IFOOD CLUB", new BigDecimal("29.90"),
                    LocalDate.of(2026, 1, 15), cat,
                    "Nubank", "1234", YearMonth.of(2026, 2), 1, 1
            );

            ExpenseSearchResponse response = new ExpenseSearchResponse(
                    List.of(result), 1, 0, 1, new BigDecimal("29.90")
            );

            when(searchService.search(any(User.class), any())).thenReturn(response);

            mockMvc.perform(get("/api/expenses/search")
                            .param("query", "ifood")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.totalAmount").value(29.90))
                    .andExpect(jsonPath("$.results[0].description").value("IFOOD CLUB"))
                    .andExpect(jsonPath("$.results[0].cardName").value("Nubank"))
                    .andExpect(jsonPath("$.results[0].amount").value(29.90));
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should return empty results when no matches")
        void shouldReturnEmptyResults() throws Exception {
            ExpenseSearchResponse response = new ExpenseSearchResponse(
                    List.of(), 0, 0, 0, BigDecimal.ZERO
            );

            when(searchService.search(any(User.class), any())).thenReturn(response);

            mockMvc.perform(get("/api/expenses/search")
                            .param("query", "nonexistent")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(0))
                    .andExpect(jsonPath("$.results").isEmpty());
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should accept all filter parameters")
        void shouldAcceptAllFilterParameters() throws Exception {
            ExpenseSearchResponse response = new ExpenseSearchResponse(
                    List.of(), 0, 0, 0, BigDecimal.ZERO
            );

            when(searchService.search(any(User.class), any())).thenReturn(response);

            mockMvc.perform(get("/api/expenses/search")
                            .param("query", "uber")
                            .param("categoryId", "5")
                            .param("cardId", "3")
                            .param("dateFrom", "2026-01-01")
                            .param("dateTo", "2026-03-31")
                            .param("amountMin", "10.00")
                            .param("amountMax", "500.00")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should search with filters only, no query text")
        void shouldSearchWithoutQuery() throws Exception {
            ExpenseSearchResponse response = new ExpenseSearchResponse(
                    List.of(), 0, 0, 0, BigDecimal.ZERO
            );

            when(searchService.search(any(User.class), any())).thenReturn(response);

            mockMvc.perform(get("/api/expenses/search")
                            .param("categoryId", "1")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should return paginated results with correct metadata")
        void shouldReturnPaginatedResults() throws Exception {
            CategoryResponse cat = new CategoryResponse(1L, "Transport", "#0000FF", null, null);
            List<ExpenseSearchResult> results = List.of(
                    new ExpenseSearchResult(1L, 1L, "UBER TRIP", new BigDecimal("15.00"),
                            LocalDate.of(2026, 2, 10), cat, "Card A", "9999",
                            YearMonth.of(2026, 3), 1, 1),
                    new ExpenseSearchResult(2L, 1L, "UBER TRIP", new BigDecimal("22.50"),
                            LocalDate.of(2026, 2, 12), cat, "Card A", "9999",
                            YearMonth.of(2026, 3), 1, 1)
            );

            ExpenseSearchResponse response = new ExpenseSearchResponse(
                    results, 45, 2, 3, new BigDecimal("675.00")
            );

            when(searchService.search(any(User.class), any())).thenReturn(response);

            mockMvc.perform(get("/api/expenses/search")
                            .param("query", "uber")
                            .param("page", "2")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalResults").value(45))
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.totalAmount").value(675.00))
                    .andExpect(jsonPath("$.results.length()").value(2));
        }

        @Test
        @WithMockUser(username = "john@test.com")
        @DisplayName("Should include installment info in results")
        void shouldIncludeInstallmentInfo() throws Exception {
            CategoryResponse cat = new CategoryResponse(2L, "Shopping", "#00FF00", null, null);
            ExpenseSearchResult result = new ExpenseSearchResult(
                    20L, 8L, "AMAZON COMPRA", new BigDecimal("166.50"),
                    LocalDate.of(2026, 1, 5), cat,
                    "Visa", "4321", YearMonth.of(2026, 2), 3, 6
            );

            ExpenseSearchResponse response = new ExpenseSearchResponse(
                    List.of(result), 1, 0, 1, new BigDecimal("166.50")
            );

            when(searchService.search(any(User.class), any())).thenReturn(response);

            mockMvc.perform(get("/api/expenses/search")
                            .param("query", "amazon")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[0].installments").value(3))
                    .andExpect(jsonPath("$.results[0].totalInstallments").value(6))
                    .andExpect(jsonPath("$.results[0].lastFourDigits").value("4321"));
        }
    }
}
