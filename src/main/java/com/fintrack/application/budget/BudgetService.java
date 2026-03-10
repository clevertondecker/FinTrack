package com.fintrack.application.budget;

import com.fintrack.domain.budget.Budget;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.ExpenseReportService;
import com.fintrack.domain.user.User;
import com.fintrack.dto.budget.BudgetResponse;
import com.fintrack.dto.budget.BudgetStatusResponse;
import com.fintrack.dto.budget.CreateBudgetRequest;
import com.fintrack.dto.budget.UpdateBudgetRequest;
import com.fintrack.dto.creditcard.CategoryResponse;
import com.fintrack.infrastructure.persistence.budget.BudgetJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BudgetService {

    private static final BigDecimal OVER_BUDGET_THRESHOLD = BigDecimal.valueOf(100);
    private static final BigDecimal NEAR_LIMIT_THRESHOLD = BigDecimal.valueOf(80);
    private static final int UTILIZATION_SCALE = 1;

    private final BudgetJpaRepository budgetRepository;
    private final CategoryJpaRepository categoryRepository;
    private final ExpenseReportService expenseReportService;

    public BudgetService(final BudgetJpaRepository budgetRepository,
                         final CategoryJpaRepository categoryRepository,
                         final ExpenseReportService expenseReportService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.expenseReportService = expenseReportService;
    }

    public BudgetResponse create(final User owner, final CreateBudgetRequest request) {
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Category not found: " + request.categoryId()));
        }

        Budget budget;
        if (request.month() != null) {
            budget = Budget.of(owner, category, request.limitAmount(), request.month());
        } else {
            budget = Budget.recurring(owner, category, request.limitAmount());
        }

        budget = budgetRepository.save(budget);
        return toResponse(budget);
    }

    public BudgetResponse update(final User owner, final Long budgetId,
                                  final UpdateBudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndOwner(budgetId, owner)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Budget not found: " + budgetId));
        budget.updateLimit(request.limitAmount());
        budget = budgetRepository.save(budget);
        return toResponse(budget);
    }

    public void delete(final User owner, final Long budgetId) {
        Budget budget = budgetRepository.findByIdAndOwner(budgetId, owner)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Budget not found: " + budgetId));
        budget.deactivate();
        budgetRepository.save(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetStatusResponse> getStatusForMonth(
            final User owner, final YearMonth month) {

        List<Budget> budgets = budgetRepository.findByOwnerAndActiveTrue(owner).stream()
                .filter(b -> b.isRecurring() || month.equals(b.getMonth()))
                .toList();
        Map<Category, BigDecimal> expenses =
                expenseReportService.getExpensesByCategory(owner, month);

        BigDecimal totalSpent = expenses.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BudgetStatusResponse> result = new ArrayList<>();
        for (Budget budget : budgets) {
            BigDecimal spent;
            CategoryResponse catResp;

            if (budget.getCategory() == null) {
                spent = totalSpent;
                catResp = null;
            } else {
                spent = expenses.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
                catResp = CategoryResponse.from(budget.getCategory());
            }

            BigDecimal remaining = budget.getLimitAmount().subtract(spent);
            BigDecimal utilization = budget.getLimitAmount()
                    .compareTo(BigDecimal.ZERO) > 0
                    ? spent.multiply(BigDecimal.valueOf(100))
                        .divide(budget.getLimitAmount(), UTILIZATION_SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            String status = resolveStatus(utilization);

            result.add(new BudgetStatusResponse(
                    budget.getId(), catResp, budget.getLimitAmount(),
                    spent, remaining, utilization, status));
        }

        return result;
    }

    private String resolveStatus(final BigDecimal utilization) {
        if (utilization.compareTo(OVER_BUDGET_THRESHOLD) >= 0) {
            return "OVER_BUDGET";
        } else if (utilization.compareTo(NEAR_LIMIT_THRESHOLD) >= 0) {
            return "NEAR_LIMIT";
        }
        return "UNDER_BUDGET";
    }

    private BudgetResponse toResponse(final Budget budget) {
        CategoryResponse catResp = budget.getCategory() != null
                ? CategoryResponse.from(budget.getCategory())
                : null;
        return new BudgetResponse(
                budget.getId(),
                catResp,
                budget.getLimitAmount(),
                budget.getMonth(),
                budget.isRecurring(),
                budget.isActive()
        );
    }
}
