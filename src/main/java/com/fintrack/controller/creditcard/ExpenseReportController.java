package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.ExpenseReportService;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.CategoryExpenseSummary;
import com.fintrack.dto.creditcard.CategoryResponse;
import com.fintrack.dto.creditcard.ExpenseByCategoryResponse;
import com.fintrack.dto.creditcard.ExpenseDetailResponse;
import com.fintrack.dto.creditcard.ExpenseReportResponse;
import com.fintrack.dto.creditcard.ExpenseTrendsResponse;
import com.fintrack.dto.creditcard.MonthlyExpenseResponse;
import com.fintrack.dto.creditcard.TopExpenseItemResponse;
import com.fintrack.dto.creditcard.TopExpensesResponse;
import com.fintrack.dto.user.UserResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for expense reports by category.
 * Provides endpoints for generating expense reports grouped by category.
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseReportController {

    /** The expense report service. */
    private final ExpenseReportService expenseReportService;
    /** The invoice service. */
    private final InvoiceService invoiceService;

    /**
     * Constructs a new ExpenseReportController.
     *
     * @param expenseReportService the expense report service. Must not be null.
     * @param invoiceService the invoice service. Must not be null.
     */
    public ExpenseReportController(
            final ExpenseReportService expenseReportService,
            final InvoiceService invoiceService) {
        this.expenseReportService = expenseReportService;
        this.invoiceService = invoiceService;
    }

    /**
     * Gets an expense report by category for the current user.
     *
     * @param month the month to generate the report for (format: yyyy-MM). Optional, defaults to current month.
     * @param categoryId optional category ID to filter by a specific category.
     * @param showTotal if true, show total expenses (all users) instead of just user's share.
     *                  Only available for card owners.
     * @param userDetails the authenticated user details.
     * @return a response with expenses grouped by category.
     */
    @GetMapping("/by-category")
    public ResponseEntity<ExpenseReportResponse> getExpensesByCategory(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean showTotal,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);

        // Default to current month if not provided
        if (month == null) {
            month = YearMonth.now();
        }

        // Get expenses by category (optionally showing total for card owners)
        Map<Category, BigDecimal> expensesByCategory = showTotal
            ? expenseReportService.getTotalExpensesByCategory(user, month)
            : expenseReportService.getExpensesByCategory(user, month);

        // Filter by specific category if requested
        if (categoryId != null) {
            Optional<Category> categoryOpt = expensesByCategory.keySet().stream()
                    .filter(c -> c.getId() != null && c.getId().equals(categoryId))
                    .findFirst();
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.ok(buildEmptyReport(user, month));
            }
            Category category = categoryOpt.get();
            expensesByCategory = Map.of(category, expensesByCategory.get(category));
        }

        // Convert to response DTOs with details
        List<ExpenseByCategoryResponse> categoryExpenses = new ArrayList<>();
        for (Map.Entry<Category, BigDecimal> entry : expensesByCategory.entrySet()) {
            Category category = entry.getKey();
            BigDecimal amount = entry.getValue();

            // Get detailed expense information for this category
            List<ExpenseDetailResponse> details = showTotal
                ? expenseReportService.getTotalExpenseDetails(user, month, category)
                : expenseReportService.getExpenseDetails(user, month, category);
            
            ExpenseByCategoryResponse expenseResponse = new ExpenseByCategoryResponse(
                    CategoryResponse.from(category),
                    amount,
                    details.size(),
                    details
            );
            categoryExpenses.add(expenseResponse);
        }

        // Sort by total amount descending
        categoryExpenses.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

        BigDecimal totalAmount = showTotal
            ? expenseReportService.getGrandTotalExpenses(user, month)
            : expenseReportService.getTotalExpenses(user, month);

        ExpenseReportResponse response = new ExpenseReportResponse(
                toUserResponse(user),
                month,
                categoryExpenses,
                totalAmount
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets expense summary for the current user.
     *
     * @param month the month to generate the summary for (format: yyyy-MM). Optional, defaults to current month.
     * @param userDetails the authenticated user details.
     * @return a response with expense summary.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getExpenseSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);

        // Default to current month if not provided
        if (month == null) {
            month = YearMonth.now();
        }

        BigDecimal totalAmount = expenseReportService.getTotalExpenses(user, month);
        Map<Category, BigDecimal> expensesByCategory = expenseReportService.getExpensesByCategory(user, month);

        List<CategoryExpenseSummary> summaries = new ArrayList<>();
        for (Map.Entry<Category, BigDecimal> entry : expensesByCategory.entrySet()) {
            Category category = entry.getKey();
            BigDecimal amount = entry.getValue();

            // Get transaction count for this category
            int transactionCount = expenseReportService.getExpenseDetails(user, month, category).size();
            
            summaries.add(new CategoryExpenseSummary(
                    category.getId(),
                    category.getName(),
                    category.getColor(),
                    amount,
                    transactionCount
            ));
        }

        // Sort by total amount descending
        summaries.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

        Map<String, Object> response = Map.of(
                "user", toUserResponse(user),
                "month", month.toString(),
                "totalAmount", totalAmount,
                "expensesByCategory", summaries
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Gets expense trends across multiple months for the current user.
     *
     * @param months the number of months to include (default 6, max 12).
     * @param showTotal if true, show total expenses instead of user's share.
     * @param userDetails the authenticated user details.
     * @return a response with monthly expense breakdowns.
     */
    @GetMapping("/trends")
    public ResponseEntity<ExpenseTrendsResponse> getExpenseTrends(
            @RequestParam(required = false, defaultValue = "6") int months,
            @RequestParam(required = false, defaultValue = "false") boolean showTotal,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);

        months = Math.max(1, Math.min(months, 12));
        YearMonth to = YearMonth.now();
        YearMonth from = to.minusMonths(months - 1);

        Map<YearMonth, Map<Category, BigDecimal>> data = showTotal
            ? expenseReportService.getTotalExpensesByMonthAndCategory(user, from, to)
            : expenseReportService.getExpensesByMonthAndCategory(user, from, to);

        List<MonthlyExpenseResponse> monthlyResponses = new ArrayList<>();
        for (Map.Entry<YearMonth, Map<Category, BigDecimal>> entry : data.entrySet()) {
            YearMonth month = entry.getKey();
            Map<Category, BigDecimal> categoryMap = entry.getValue();

            BigDecimal monthTotal = categoryMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<CategoryExpenseSummary> categories = categoryMap.entrySet().stream()
                .map(e -> new CategoryExpenseSummary(
                    e.getKey().getId(),
                    e.getKey().getName(),
                    e.getKey().getColor(),
                    e.getValue(),
                    0
                ))
                .sorted((a, b) -> b.totalAmount().compareTo(a.totalAmount()))
                .toList();

            monthlyResponses.add(new MonthlyExpenseResponse(month, monthTotal, categories));
        }

        return ResponseEntity.ok(new ExpenseTrendsResponse(toUserResponse(user), monthlyResponses));
    }

    /**
     * Gets the top expenses for the current user in a given month.
     *
     * @param month the month to get top expenses for (format: yyyy-MM). Defaults to current month.
     * @param limit the maximum number of items to return (default 5, max 20).
     * @param showTotal if true, show total expenses instead of user's share.
     * @param userDetails the authenticated user details.
     * @return a response with the top expense items.
     */
    @GetMapping("/top")
    public ResponseEntity<TopExpensesResponse> getTopExpenses(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false, defaultValue = "5") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean showTotal,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);

        if (month == null) {
            month = YearMonth.now();
        }
        limit = Math.max(1, Math.min(limit, 20));

        List<ExpenseReportService.TopExpenseEntry> entries = showTotal
            ? expenseReportService.getTotalTopExpenses(user, month, limit)
            : expenseReportService.getTopExpenses(user, month, limit);

        BigDecimal totalAmount = showTotal
            ? expenseReportService.getGrandTotalExpenses(user, month)
            : expenseReportService.getTotalExpenses(user, month);

        List<TopExpenseItemResponse> topItems = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ExpenseReportService.TopExpenseEntry entry = entries.get(i);
            BigDecimal percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? entry.amount().multiply(BigDecimal.valueOf(100))
                    .divide(totalAmount, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            topItems.add(new TopExpenseItemResponse(
                i + 1,
                entry.itemId(),
                entry.description(),
                entry.amount(),
                entry.purchaseDate(),
                entry.invoiceId(),
                CategoryResponse.from(entry.category()),
                percentage
            ));
        }

        return ResponseEntity.ok(new TopExpensesResponse(
            toUserResponse(user), month, totalAmount, topItems));
    }

    /**
     * Resolves the authenticated user from the security context.
     *
     * @param userDetails the authenticated user details. Must not be null.
     * @return the resolved User entity. Never null.
     * @throws IllegalArgumentException if the user is not found.
     */
    private User resolveUser(final UserDetails userDetails) {
        return invoiceService.findUserByUsername(userDetails.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /**
     * Converts a User domain entity to UserResponse DTO.
     *
     * @param user the user to convert. Must not be null.
     * @return the UserResponse DTO. Never null.
     */
    private UserResponse toUserResponse(final User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail().getEmail(),
                user.getRoles().stream()
                        .map(Enum::name)
                        .toArray(String[]::new),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * Builds an empty expense report response.
     *
     * @param user the user. Must not be null.
     * @param month the month. Must not be null.
     * @return an empty ExpenseReportResponse. Never null.
     */
    private ExpenseReportResponse buildEmptyReport(final User user, final YearMonth month) {
        return new ExpenseReportResponse(
                toUserResponse(user),
                month,
                List.of(),
                BigDecimal.ZERO
        );
    }
}

