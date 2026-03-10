package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.ExpenseReportService;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.CategoryExpenseSummary;
import com.fintrack.dto.creditcard.CategoryResponse;
import com.fintrack.dto.creditcard.ExpenseByCardResponse;
import com.fintrack.dto.creditcard.ExpenseByRecurrenceResponse;
import com.fintrack.dto.creditcard.ExpenseByCategoryResponse;
import com.fintrack.dto.creditcard.PeriodComparisonResponse;
import com.fintrack.dto.creditcard.ExpenseDetailResponse;
import com.fintrack.dto.creditcard.ExpenseReportResponse;
import com.fintrack.dto.creditcard.ExpenseTrendsResponse;
import com.fintrack.dto.creditcard.MonthlyExpenseResponse;
import com.fintrack.dto.creditcard.TopExpenseItemResponse;
import com.fintrack.dto.creditcard.TopExpensesResponse;
import com.fintrack.dto.dashboard.DailyExpenseResponse;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

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

        BigDecimal totalAmount = showTotal
            ? expenseReportService.getGrandTotalExpenses(user, month)
            : expenseReportService.getTotalExpenses(user, month);

        List<ExpenseByCategoryResponse> categoryExpenses = new ArrayList<>();
        for (Map.Entry<Category, BigDecimal> entry : expensesByCategory.entrySet()) {
            Category category = entry.getKey();
            BigDecimal amount = entry.getValue();

            List<ExpenseDetailResponse> details = showTotal
                ? expenseReportService.getTotalExpenseDetails(user, month, category)
                : expenseReportService.getExpenseDetails(user, month, category);

            BigDecimal percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? amount.multiply(BigDecimal.valueOf(100))
                    .divide(totalAmount, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            Map<java.time.LocalDate, BigDecimal> dailyMap = new TreeMap<>();
            for (ExpenseDetailResponse detail : details) {
                if (detail.purchaseDate() != null) {
                    dailyMap.merge(detail.purchaseDate(), detail.amount(), BigDecimal::add);
                }
            }
            List<DailyExpenseResponse> dailyBreakdown = dailyMap.entrySet().stream()
                .map(e -> new DailyExpenseResponse(e.getKey(), e.getValue()))
                .toList();

            categoryExpenses.add(new ExpenseByCategoryResponse(
                    CategoryResponse.from(category),
                    amount,
                    percentage,
                    details.size(),
                    details,
                    dailyBreakdown
            ));
        }

        categoryExpenses.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

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

        return ResponseEntity.ok(
                buildTrendsResponse(toUserResponse(user), monthlyResponses));
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

    @GetMapping("/by-card")
    public ResponseEntity<List<ExpenseByCardResponse>> getExpensesByCard(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        if (month == null) {
            month = YearMonth.now();
        }

        List<ExpenseReportService.CardExpenseEntry> entries =
                expenseReportService.getExpensesByCard(user, month);

        BigDecimal grandTotal = entries.stream()
                .map(ExpenseReportService.CardExpenseEntry::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ExpenseByCardResponse> response = entries.stream()
                .map(entry -> {
                    BigDecimal pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                        ? entry.totalAmount().multiply(BigDecimal.valueOf(100))
                            .divide(grandTotal, 1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                    List<CategoryExpenseSummary> categories = entry.categoryBreakdown()
                            .entrySet().stream()
                            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                            .map(e -> new CategoryExpenseSummary(
                                    e.getKey().getId(),
                                    e.getKey().getName(),
                                    e.getKey().getColor(),
                                    e.getValue(),
                                    0
                            ))
                            .toList();

                    return new ExpenseByCardResponse(
                            entry.cardId(), entry.cardName(), entry.lastFourDigits(),
                            entry.bankName(), entry.totalAmount(), pct,
                            entry.transactionCount(), categories
                    );
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-recurrence")
    public ResponseEntity<List<ExpenseByRecurrenceResponse>> getExpensesByRecurrence(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        if (month == null) {
            month = YearMonth.now();
        }

        List<ExpenseReportService.RecurrenceExpenseEntry> entries =
                expenseReportService.getExpensesByRecurrence(user, month);

        BigDecimal grandTotal = entries.stream()
                .map(ExpenseReportService.RecurrenceExpenseEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ExpenseByRecurrenceResponse> response = entries.stream()
                .map(entry -> {
                    BigDecimal pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                        ? entry.amount().multiply(BigDecimal.valueOf(100))
                            .divide(grandTotal, 1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                    return new ExpenseByRecurrenceResponse(
                            entry.type(), entry.amount(), pct, entry.transactionCount());
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/comparison")
    public ResponseEntity<PeriodComparisonResponse> compareExpenses(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth compareTo,
            @RequestParam(required = false, defaultValue = "false") boolean showTotal,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);

        Map<Category, BigDecimal> currentCats = showTotal
                ? expenseReportService.getTotalExpensesByCategory(user, month)
                : expenseReportService.getExpensesByCategory(user, month);
        Map<Category, BigDecimal> compareCats = showTotal
                ? expenseReportService.getTotalExpensesByCategory(user, compareTo)
                : expenseReportService.getExpensesByCategory(user, compareTo);

        BigDecimal currentTotal = currentCats.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal compareTotal = compareCats.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int currentCount = showTotal
                ? countTotalItems(user, month)
                : countUserItems(user, month);
        int compareCount = showTotal
                ? countTotalItems(user, compareTo)
                : countUserItems(user, compareTo);

        BigDecimal diffAmount = currentTotal.subtract(compareTotal);
        BigDecimal diffPct = compareTotal.compareTo(BigDecimal.ZERO) > 0
                ? diffAmount.multiply(BigDecimal.valueOf(100))
                    .divide(compareTotal, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Set<Category> allCategories = new LinkedHashSet<>();
        allCategories.addAll(currentCats.keySet());
        allCategories.addAll(compareCats.keySet());

        List<PeriodComparisonResponse.CategoryComparison> catComps = allCategories.stream()
                .map(cat -> {
                    BigDecimal cur = currentCats.getOrDefault(cat, BigDecimal.ZERO);
                    BigDecimal cmp = compareCats.getOrDefault(cat, BigDecimal.ZERO);
                    BigDecimal catDiff = cur.subtract(cmp);
                    BigDecimal catDiffPct = cmp.compareTo(BigDecimal.ZERO) > 0
                            ? catDiff.multiply(BigDecimal.valueOf(100))
                                .divide(cmp, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return new PeriodComparisonResponse.CategoryComparison(
                            CategoryResponse.from(cat), cur, cmp, catDiff, catDiffPct);
                })
                .sorted((a, b) -> b.currentAmount().compareTo(a.currentAmount()))
                .toList();

        return ResponseEntity.ok(new PeriodComparisonResponse(
                new PeriodComparisonResponse.MonthSummary(month, currentTotal, currentCount),
                new PeriodComparisonResponse.MonthSummary(compareTo, compareTotal, compareCount),
                diffAmount, diffPct, catComps
        ));
    }

    private ExpenseTrendsResponse buildTrendsResponse(
            final UserResponse userResp,
            final List<MonthlyExpenseResponse> monthlyResponses) {

        if (monthlyResponses.isEmpty()) {
            return new ExpenseTrendsResponse(userResp, monthlyResponses,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal highest = BigDecimal.ZERO;
        BigDecimal lowest = monthlyResponses.get(0).totalAmount();

        for (MonthlyExpenseResponse mr : monthlyResponses) {
            sum = sum.add(mr.totalAmount());
            if (mr.totalAmount().compareTo(highest) > 0) {
                highest = mr.totalAmount();
            }
            if (mr.totalAmount().compareTo(lowest) < 0) {
                lowest = mr.totalAmount();
            }
        }

        BigDecimal avg = sum.divide(
                BigDecimal.valueOf(monthlyResponses.size()), 2, RoundingMode.HALF_UP);

        BigDecimal currentTotal =
                monthlyResponses.get(monthlyResponses.size() - 1).totalAmount();

        BigDecimal currentVsAvg = percentChange(currentTotal, avg);

        BigDecimal previousTotal = monthlyResponses.size() >= 2
                ? monthlyResponses.get(monthlyResponses.size() - 2).totalAmount()
                : BigDecimal.ZERO;

        BigDecimal currentVsPrev = percentChange(currentTotal, previousTotal);

        return new ExpenseTrendsResponse(userResp, monthlyResponses,
                avg, currentVsAvg, currentVsPrev, highest, lowest);
    }

    private BigDecimal percentChange(final BigDecimal current, final BigDecimal base) {
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(base)
                .multiply(BigDecimal.valueOf(100))
                .divide(base, 1, RoundingMode.HALF_UP);
    }

    private int countUserItems(final User user, final YearMonth month) {
        Map<Category, BigDecimal> cats = expenseReportService.getExpensesByCategory(user, month);
        int count = 0;
        for (Category cat : cats.keySet()) {
            count += expenseReportService.getExpenseDetails(user, month, cat).size();
        }
        return count;
    }

    private int countTotalItems(final User user, final YearMonth month) {
        Map<Category, BigDecimal> cats =
                expenseReportService.getTotalExpensesByCategory(user, month);
        int count = 0;
        for (Category cat : cats.keySet()) {
            count += expenseReportService.getTotalExpenseDetails(user, month, cat).size();
        }
        return count;
    }

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

