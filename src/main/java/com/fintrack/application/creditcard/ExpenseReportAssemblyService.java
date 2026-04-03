package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.ExpenseReportService;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.CategoryExpenseSummary;
import com.fintrack.dto.creditcard.CategoryResponse;
import com.fintrack.dto.creditcard.ExpenseByCardResponse;
import com.fintrack.dto.creditcard.ExpenseByRecurrenceResponse;
import com.fintrack.dto.creditcard.ExpenseByCategoryResponse;
import com.fintrack.dto.creditcard.ExpenseDetailResponse;
import com.fintrack.dto.creditcard.ExpenseReportResponse;
import com.fintrack.dto.creditcard.ExpenseTrendsResponse;
import com.fintrack.dto.creditcard.MonthlyExpenseResponse;
import com.fintrack.dto.creditcard.PeriodComparisonResponse;
import com.fintrack.dto.creditcard.TopExpenseItemResponse;
import com.fintrack.dto.creditcard.TopExpensesResponse;
import com.fintrack.dto.dashboard.DailyExpenseResponse;
import com.fintrack.dto.user.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class ExpenseReportAssemblyService {

    private final ExpenseReportService expenseReportService;

    public ExpenseReportAssemblyService(final ExpenseReportService expenseReportService) {
        this.expenseReportService = expenseReportService;
    }

    public ExpenseReportResponse buildCategoryReport(
            final User user, final YearMonth month, final boolean showTotal) {

        Map<Category, BigDecimal> expensesByCategory = showTotal
                ? expenseReportService.getTotalExpensesByCategory(user, month)
                : expenseReportService.getExpensesByCategory(user, month);

        BigDecimal totalAmount = showTotal
                ? expenseReportService.getGrandTotalExpenses(user, month)
                : expenseReportService.getTotalExpenses(user, month);

        List<ExpenseByCategoryResponse> categoryExpenses =
                buildCategoryBreakdown(user, month, expensesByCategory, totalAmount, showTotal);

        categoryExpenses.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

        return new ExpenseReportResponse(toUserResponse(user), month, categoryExpenses, totalAmount);
    }

    public ExpenseReportResponse buildFilteredCategoryReport(
            final User user, final YearMonth month,
            final Long categoryId, final boolean showTotal) {

        Map<Category, BigDecimal> expensesByCategory = showTotal
                ? expenseReportService.getTotalExpensesByCategory(user, month)
                : expenseReportService.getExpensesByCategory(user, month);

        Category matchedCategory = expensesByCategory.keySet().stream()
                .filter(c -> c.getId() != null && c.getId().equals(categoryId))
                .findFirst()
                .orElse(null);

        if (matchedCategory == null) {
            return buildEmptyReport(user, month);
        }

        Map<Category, BigDecimal> filtered = Map.of(matchedCategory,
                expensesByCategory.get(matchedCategory));

        BigDecimal totalAmount = showTotal
                ? expenseReportService.getGrandTotalExpenses(user, month)
                : expenseReportService.getTotalExpenses(user, month);

        List<ExpenseByCategoryResponse> categoryExpenses =
                buildCategoryBreakdown(user, month, filtered, totalAmount, showTotal);

        return new ExpenseReportResponse(toUserResponse(user), month, categoryExpenses, totalAmount);
    }

    public Map<String, Object> buildSummary(final User user, final YearMonth month) {
        BigDecimal totalAmount = expenseReportService.getTotalExpenses(user, month);
        Map<Category, BigDecimal> expensesByCategory =
                expenseReportService.getExpensesByCategory(user, month);

        List<CategoryExpenseSummary> summaries = new ArrayList<>();
        for (Map.Entry<Category, BigDecimal> entry : expensesByCategory.entrySet()) {
            Category category = entry.getKey();
            BigDecimal amount = entry.getValue();
            int txCount = expenseReportService.getExpenseDetails(user, month, category).size();
            summaries.add(new CategoryExpenseSummary(
                    category.getId(), category.getName(), category.getColor(), amount, txCount));
        }
        summaries.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

        return Map.of(
                "user", toUserResponse(user),
                "month", month.toString(),
                "totalAmount", totalAmount,
                "expensesByCategory", summaries);
    }

    public ExpenseTrendsResponse buildTrends(
            final User user, final int months, final boolean showTotal) {

        int clamped = Math.max(1, Math.min(months, 12));
        YearMonth to = YearMonth.now();
        YearMonth from = to.minusMonths(clamped - 1);

        Map<YearMonth, Map<Category, BigDecimal>> data = showTotal
                ? expenseReportService.getTotalExpensesByMonthAndCategory(user, from, to)
                : expenseReportService.getExpensesByMonthAndCategory(user, from, to);

        List<MonthlyExpenseResponse> monthlyResponses = new ArrayList<>();
        for (Map.Entry<YearMonth, Map<Category, BigDecimal>> entry : data.entrySet()) {
            Map<Category, BigDecimal> categoryMap = entry.getValue();
            BigDecimal monthTotal = categoryMap.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<CategoryExpenseSummary> categories = categoryMap.entrySet().stream()
                    .map(e -> new CategoryExpenseSummary(
                            e.getKey().getId(), e.getKey().getName(),
                            e.getKey().getColor(), e.getValue(), 0))
                    .sorted((a, b) -> b.totalAmount().compareTo(a.totalAmount()))
                    .toList();
            monthlyResponses.add(new MonthlyExpenseResponse(entry.getKey(), monthTotal, categories));
        }

        return computeTrendsStats(toUserResponse(user), monthlyResponses);
    }

    public TopExpensesResponse buildTopExpenses(
            final User user, final YearMonth month,
            final int limit, final boolean showTotal) {

        int clamped = Math.max(1, Math.min(limit, 20));

        List<ExpenseReportService.TopExpenseEntry> entries = showTotal
                ? expenseReportService.getTotalTopExpenses(user, month, clamped)
                : expenseReportService.getTopExpenses(user, month, clamped);

        BigDecimal totalAmount = showTotal
                ? expenseReportService.getGrandTotalExpenses(user, month)
                : expenseReportService.getTotalExpenses(user, month);

        List<TopExpenseItemResponse> topItems = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ExpenseReportService.TopExpenseEntry entry = entries.get(i);
            BigDecimal pct = percentChange(entry.amount(), totalAmount);
            topItems.add(new TopExpenseItemResponse(
                    i + 1, entry.itemId(), entry.description(), entry.amount(),
                    entry.purchaseDate(), entry.invoiceId(),
                    CategoryResponse.from(entry.category()), pct));
        }

        return new TopExpensesResponse(toUserResponse(user), month, totalAmount, topItems);
    }

    public List<ExpenseByCardResponse> buildByCard(final User user, final YearMonth month) {
        List<ExpenseReportService.CardExpenseEntry> entries =
                expenseReportService.getExpensesByCard(user, month);

        BigDecimal grandTotal = entries.stream()
                .map(ExpenseReportService.CardExpenseEntry::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return entries.stream()
                .map(entry -> {
                    BigDecimal pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                            ? entry.totalAmount().multiply(BigDecimal.valueOf(100))
                                .divide(grandTotal, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    List<CategoryExpenseSummary> categories = entry.categoryBreakdown()
                            .entrySet().stream()
                            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                            .map(e -> new CategoryExpenseSummary(
                                    e.getKey().getId(), e.getKey().getName(),
                                    e.getKey().getColor(), e.getValue(), 0))
                            .toList();

                    return new ExpenseByCardResponse(
                            entry.cardId(), entry.cardName(), entry.lastFourDigits(),
                            entry.bankName(), entry.totalAmount(), pct,
                            entry.transactionCount(), categories);
                })
                .toList();
    }

    public List<ExpenseByRecurrenceResponse> buildByRecurrence(
            final User user, final YearMonth month) {

        List<ExpenseReportService.RecurrenceExpenseEntry> entries =
                expenseReportService.getExpensesByRecurrence(user, month);

        BigDecimal grandTotal = entries.stream()
                .map(ExpenseReportService.RecurrenceExpenseEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return entries.stream()
                .map(entry -> {
                    BigDecimal pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                            ? entry.amount().multiply(BigDecimal.valueOf(100))
                                .divide(grandTotal, 1, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return new ExpenseByRecurrenceResponse(
                            entry.type(), entry.amount(), pct, entry.transactionCount());
                })
                .toList();
    }

    public PeriodComparisonResponse buildComparison(
            final User user, final YearMonth month,
            final YearMonth compareTo, final boolean showTotal) {

        Map<Category, BigDecimal> currentCats = showTotal
                ? expenseReportService.getTotalExpensesByCategory(user, month)
                : expenseReportService.getExpensesByCategory(user, month);
        Map<Category, BigDecimal> compareCats = showTotal
                ? expenseReportService.getTotalExpensesByCategory(user, compareTo)
                : expenseReportService.getExpensesByCategory(user, compareTo);

        BigDecimal currentTotal = sumValues(currentCats);
        BigDecimal compareTotal = sumValues(compareCats);

        int currentCount = showTotal
                ? countTotalItems(user, month) : countUserItems(user, month);
        int compareCount = showTotal
                ? countTotalItems(user, compareTo) : countUserItems(user, compareTo);

        BigDecimal diffAmount = currentTotal.subtract(compareTotal);
        BigDecimal diffPct = percentChange(diffAmount, compareTotal);

        Set<Category> allCategories = new LinkedHashSet<>();
        allCategories.addAll(currentCats.keySet());
        allCategories.addAll(compareCats.keySet());

        List<PeriodComparisonResponse.CategoryComparison> catComps = allCategories.stream()
                .map(cat -> {
                    BigDecimal cur = currentCats.getOrDefault(cat, BigDecimal.ZERO);
                    BigDecimal cmp = compareCats.getOrDefault(cat, BigDecimal.ZERO);
                    BigDecimal catDiff = cur.subtract(cmp);
                    BigDecimal catDiffPct = percentChange(catDiff, cmp);
                    return new PeriodComparisonResponse.CategoryComparison(
                            CategoryResponse.from(cat), cur, cmp, catDiff, catDiffPct);
                })
                .sorted((a, b) -> b.currentAmount().compareTo(a.currentAmount()))
                .toList();

        return new PeriodComparisonResponse(
                new PeriodComparisonResponse.MonthSummary(month, currentTotal, currentCount),
                new PeriodComparisonResponse.MonthSummary(compareTo, compareTotal, compareCount),
                diffAmount, diffPct, catComps);
    }

    // ---- private helpers ----

    private List<ExpenseByCategoryResponse> buildCategoryBreakdown(
            final User user, final YearMonth month,
            final Map<Category, BigDecimal> expensesByCategory,
            final BigDecimal totalAmount, final boolean showTotal) {

        List<ExpenseByCategoryResponse> result = new ArrayList<>();
        for (Map.Entry<Category, BigDecimal> entry : expensesByCategory.entrySet()) {
            Category category = entry.getKey();
            BigDecimal amount = entry.getValue();

            List<ExpenseDetailResponse> details = showTotal
                    ? expenseReportService.getTotalExpenseDetails(user, month, category)
                    : expenseReportService.getExpenseDetails(user, month, category);

            BigDecimal pct = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalAmount, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<LocalDate, BigDecimal> dailyMap = new TreeMap<>();
            for (ExpenseDetailResponse detail : details) {
                if (detail.purchaseDate() != null) {
                    dailyMap.merge(detail.purchaseDate(), detail.amount(), BigDecimal::add);
                }
            }
            List<DailyExpenseResponse> dailyBreakdown = dailyMap.entrySet().stream()
                    .map(e -> new DailyExpenseResponse(e.getKey(), e.getValue()))
                    .toList();

            result.add(new ExpenseByCategoryResponse(
                    CategoryResponse.from(category), amount, pct,
                    details.size(), details, dailyBreakdown));
        }
        return result;
    }

    private ExpenseTrendsResponse computeTrendsStats(
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

    private BigDecimal sumValues(final Map<Category, BigDecimal> map) {
        return map.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private UserResponse toUserResponse(final User user) {
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail().getEmail(),
                user.getRoles().stream().map(Enum::name).toArray(String[]::new),
                user.getCreatedAt(), user.getUpdatedAt());
    }

    private ExpenseReportResponse buildEmptyReport(final User user, final YearMonth month) {
        return new ExpenseReportResponse(toUserResponse(user), month, List.of(), BigDecimal.ZERO);
    }
}
