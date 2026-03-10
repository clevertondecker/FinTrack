package com.fintrack.application.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fintrack.domain.budget.Budget;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.ExpenseReportService;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.budget.BudgetResponse;
import com.fintrack.dto.budget.BudgetStatusResponse;
import com.fintrack.dto.budget.CreateBudgetRequest;
import com.fintrack.dto.budget.UpdateBudgetRequest;
import com.fintrack.infrastructure.persistence.budget.BudgetJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService Tests")
class BudgetServiceTest {

    @Mock
    private BudgetJpaRepository budgetRepository;

    @Mock
    private CategoryJpaRepository categoryRepository;

    @Mock
    private ExpenseReportService expenseReportService;

    private BudgetService budgetService;
    private User testUser;
    private Category foodCategory;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(budgetRepository, categoryRepository, expenseReportService);
        testUser = User.createLocalUser("John", "john@test.com", "pass123", Set.of(Role.USER));
        foodCategory = Category.of("Food", "#FF0000");
        ReflectionTestUtils.setField(foodCategory, "id", 1L);
    }

    @Nested
    @DisplayName("create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create monthly budget with category")
        void shouldCreateMonthlyBudgetWithCategory() {
            YearMonth month = YearMonth.of(2026, 3);
            CreateBudgetRequest request = new CreateBudgetRequest(1L, new BigDecimal("1000.00"), month);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(foodCategory));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
                Budget b = inv.getArgument(0);
                ReflectionTestUtils.setField(b, "id", 10L);
                return b;
            });

            BudgetResponse response = budgetService.create(testUser, request);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.category()).isNotNull();
            assertThat(response.category().name()).isEqualTo("Food");
            assertThat(response.limitAmount()).isEqualByComparingTo("1000.00");
            assertThat(response.month()).isEqualTo(month);
            assertThat(response.recurring()).isFalse();
            assertThat(response.active()).isTrue();
        }

        @Test
        @DisplayName("Should create recurring budget without month")
        void shouldCreateRecurringBudget() {
            CreateBudgetRequest request = new CreateBudgetRequest(1L, new BigDecimal("500.00"), null);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(foodCategory));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
                Budget b = inv.getArgument(0);
                ReflectionTestUtils.setField(b, "id", 11L);
                return b;
            });

            BudgetResponse response = budgetService.create(testUser, request);

            assertThat(response.month()).isNull();
            assertThat(response.recurring()).isTrue();
        }

        @Test
        @DisplayName("Should create general budget without category")
        void shouldCreateGeneralBudget() {
            CreateBudgetRequest request = new CreateBudgetRequest(null, new BigDecimal("2000.00"),
                    YearMonth.of(2026, 3));

            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
                Budget b = inv.getArgument(0);
                ReflectionTestUtils.setField(b, "id", 12L);
                return b;
            });

            BudgetResponse response = budgetService.create(testUser, request);

            assertThat(response.category()).isNull();
        }

        @Test
        @DisplayName("Should throw when category not found")
        void shouldThrowWhenCategoryNotFound() {
            CreateBudgetRequest request = new CreateBudgetRequest(999L, new BigDecimal("1000.00"),
                    YearMonth.now());
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.create(testUser, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Category not found: 999");
        }
    }

    @Nested
    @DisplayName("update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update budget limit")
        void shouldUpdateBudgetLimit() {
            Budget existing = Budget.of(testUser, foodCategory, new BigDecimal("1000.00"),
                    YearMonth.of(2026, 3));
            ReflectionTestUtils.setField(existing, "id", 10L);

            when(budgetRepository.findByIdAndOwner(10L, testUser)).thenReturn(Optional.of(existing));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateBudgetRequest request = new UpdateBudgetRequest(new BigDecimal("1500.00"));
            BudgetResponse response = budgetService.update(testUser, 10L, request);

            assertThat(response.limitAmount()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("Should throw when budget not found")
        void shouldThrowWhenBudgetNotFound() {
            when(budgetRepository.findByIdAndOwner(999L, testUser)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.update(testUser, 999L,
                    new UpdateBudgetRequest(BigDecimal.TEN)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Budget not found: 999");
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should deactivate budget (soft delete)")
        void shouldDeactivateBudget() {
            Budget existing = Budget.of(testUser, foodCategory, new BigDecimal("1000.00"),
                    YearMonth.of(2026, 3));
            ReflectionTestUtils.setField(existing, "id", 10L);

            when(budgetRepository.findByIdAndOwner(10L, testUser)).thenReturn(Optional.of(existing));
            when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> inv.getArgument(0));

            budgetService.delete(testUser, 10L);

            ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
            verify(budgetRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("Should throw when budget not found for delete")
        void shouldThrowWhenBudgetNotFoundForDelete() {
            when(budgetRepository.findByIdAndOwner(999L, testUser)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.delete(testUser, 999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getStatusForMonth Tests")
    class GetStatusForMonthTests {

        @Test
        @DisplayName("Should return budget status with UNDER_BUDGET when below 80%")
        void shouldReturnUnderBudgetStatus() {
            YearMonth month = YearMonth.of(2026, 3);
            Budget budget = Budget.of(testUser, foodCategory, new BigDecimal("1000.00"), month);
            ReflectionTestUtils.setField(budget, "id", 1L);

            when(budgetRepository.findByOwnerAndActiveTrue(testUser)).thenReturn(List.of(budget));
            when(expenseReportService.getExpensesByCategory(testUser, month))
                    .thenReturn(Map.of(foodCategory, new BigDecimal("500.00")));

            List<BudgetStatusResponse> result = budgetService.getStatusForMonth(testUser, month);

            assertThat(result).hasSize(1);
            BudgetStatusResponse status = result.get(0);
            assertThat(status.budgetLimit()).isEqualByComparingTo("1000.00");
            assertThat(status.actualSpent()).isEqualByComparingTo("500.00");
            assertThat(status.remaining()).isEqualByComparingTo("500.00");
            assertThat(status.utilizationPercent()).isEqualByComparingTo("50.0");
            assertThat(status.status()).isEqualTo("UNDER_BUDGET");
        }

        @Test
        @DisplayName("Should return NEAR_LIMIT when between 80% and 100%")
        void shouldReturnNearLimitStatus() {
            YearMonth month = YearMonth.of(2026, 3);
            Budget budget = Budget.of(testUser, foodCategory, new BigDecimal("1000.00"), month);
            ReflectionTestUtils.setField(budget, "id", 1L);

            when(budgetRepository.findByOwnerAndActiveTrue(testUser)).thenReturn(List.of(budget));
            when(expenseReportService.getExpensesByCategory(testUser, month))
                    .thenReturn(Map.of(foodCategory, new BigDecimal("900.00")));

            List<BudgetStatusResponse> result = budgetService.getStatusForMonth(testUser, month);

            assertThat(result.get(0).status()).isEqualTo("NEAR_LIMIT");
            assertThat(result.get(0).utilizationPercent()).isEqualByComparingTo("90.0");
        }

        @Test
        @DisplayName("Should return OVER_BUDGET when at or above 100%")
        void shouldReturnOverBudgetStatus() {
            YearMonth month = YearMonth.of(2026, 3);
            Budget budget = Budget.of(testUser, foodCategory, new BigDecimal("1000.00"), month);
            ReflectionTestUtils.setField(budget, "id", 1L);

            when(budgetRepository.findByOwnerAndActiveTrue(testUser)).thenReturn(List.of(budget));
            when(expenseReportService.getExpensesByCategory(testUser, month))
                    .thenReturn(Map.of(foodCategory, new BigDecimal("1200.00")));

            List<BudgetStatusResponse> result = budgetService.getStatusForMonth(testUser, month);

            assertThat(result.get(0).status()).isEqualTo("OVER_BUDGET");
            assertThat(result.get(0).remaining()).isEqualByComparingTo("-200.00");
        }

        @Test
        @DisplayName("Should include recurring budgets for any month")
        void shouldIncludeRecurringBudgets() {
            YearMonth month = YearMonth.of(2026, 5);
            Budget recurring = Budget.recurring(testUser, foodCategory, new BigDecimal("1000.00"));
            ReflectionTestUtils.setField(recurring, "id", 1L);

            when(budgetRepository.findByOwnerAndActiveTrue(testUser)).thenReturn(List.of(recurring));
            when(expenseReportService.getExpensesByCategory(testUser, month))
                    .thenReturn(Map.of(foodCategory, new BigDecimal("300.00")));

            List<BudgetStatusResponse> result = budgetService.getStatusForMonth(testUser, month);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).actualSpent()).isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("Should exclude monthly budgets from different months")
        void shouldExcludeOtherMonthBudgets() {
            YearMonth month = YearMonth.of(2026, 3);
            Budget otherMonth = Budget.of(testUser, foodCategory, new BigDecimal("1000.00"),
                    YearMonth.of(2026, 2));
            ReflectionTestUtils.setField(otherMonth, "id", 1L);

            when(budgetRepository.findByOwnerAndActiveTrue(testUser)).thenReturn(List.of(otherMonth));
            when(expenseReportService.getExpensesByCategory(testUser, month)).thenReturn(Map.of());

            List<BudgetStatusResponse> result = budgetService.getStatusForMonth(testUser, month);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should use total spent for general budget (no category)")
        void shouldUseTotalSpentForGeneralBudget() {
            YearMonth month = YearMonth.of(2026, 3);
            Budget general = Budget.of(testUser, null, new BigDecimal("3000.00"), month);
            ReflectionTestUtils.setField(general, "id", 1L);

            Category transport = Category.of("Transport", "#0000FF");
            ReflectionTestUtils.setField(transport, "id", 2L);

            when(budgetRepository.findByOwnerAndActiveTrue(testUser)).thenReturn(List.of(general));
            when(expenseReportService.getExpensesByCategory(testUser, month))
                    .thenReturn(Map.of(
                            foodCategory, new BigDecimal("1000.00"),
                            transport, new BigDecimal("500.00")));

            List<BudgetStatusResponse> result = budgetService.getStatusForMonth(testUser, month);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).actualSpent()).isEqualByComparingTo("1500.00");
            assertThat(result.get(0).category()).isNull();
        }

        @Test
        @DisplayName("Should return zero spent when no expenses for category")
        void shouldReturnZeroSpentWhenNoExpenses() {
            YearMonth month = YearMonth.of(2026, 3);
            Budget budget = Budget.of(testUser, foodCategory, new BigDecimal("1000.00"), month);
            ReflectionTestUtils.setField(budget, "id", 1L);

            when(budgetRepository.findByOwnerAndActiveTrue(testUser)).thenReturn(List.of(budget));
            when(expenseReportService.getExpensesByCategory(testUser, month)).thenReturn(Map.of());

            List<BudgetStatusResponse> result = budgetService.getStatusForMonth(testUser, month);

            assertThat(result.get(0).actualSpent()).isEqualByComparingTo("0");
            assertThat(result.get(0).utilizationPercent()).isEqualByComparingTo("0");
            assertThat(result.get(0).status()).isEqualTo("UNDER_BUDGET");
        }
    }
}
