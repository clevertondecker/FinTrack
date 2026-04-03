package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.ExpenseReportAssemblyService;
import com.fintrack.controller.BaseController;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.ExpenseByCardResponse;
import com.fintrack.dto.creditcard.ExpenseByRecurrenceResponse;
import com.fintrack.dto.creditcard.PeriodComparisonResponse;
import com.fintrack.dto.creditcard.ExpenseReportResponse;
import com.fintrack.dto.creditcard.ExpenseTrendsResponse;
import com.fintrack.dto.creditcard.TopExpensesResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseReportController extends BaseController {

    private final ExpenseReportAssemblyService assemblyService;

    public ExpenseReportController(final ExpenseReportAssemblyService assemblyService) {
        this.assemblyService = assemblyService;
    }

    @GetMapping("/by-category")
    public ResponseEntity<ExpenseReportResponse> getExpensesByCategory(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean showTotal,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        YearMonth resolved = month != null ? month : YearMonth.now();

        ExpenseReportResponse response = categoryId != null
                ? assemblyService.buildFilteredCategoryReport(user, resolved, categoryId, showTotal)
                : assemblyService.buildCategoryReport(user, resolved, showTotal);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getExpenseSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        YearMonth resolved = month != null ? month : YearMonth.now();
        return ResponseEntity.ok(assemblyService.buildSummary(user, resolved));
    }

    @GetMapping("/trends")
    public ResponseEntity<ExpenseTrendsResponse> getExpenseTrends(
            @RequestParam(required = false, defaultValue = "6") int months,
            @RequestParam(required = false, defaultValue = "false") boolean showTotal,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        return ResponseEntity.ok(assemblyService.buildTrends(user, months, showTotal));
    }

    @GetMapping("/top")
    public ResponseEntity<TopExpensesResponse> getTopExpenses(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false, defaultValue = "5") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean showTotal,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        YearMonth resolved = month != null ? month : YearMonth.now();
        return ResponseEntity.ok(assemblyService.buildTopExpenses(user, resolved, limit, showTotal));
    }

    @GetMapping("/by-card")
    public ResponseEntity<List<ExpenseByCardResponse>> getExpensesByCard(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        YearMonth resolved = month != null ? month : YearMonth.now();
        return ResponseEntity.ok(assemblyService.buildByCard(user, resolved));
    }

    @GetMapping("/by-recurrence")
    public ResponseEntity<List<ExpenseByRecurrenceResponse>> getExpensesByRecurrence(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        YearMonth resolved = month != null ? month : YearMonth.now();
        return ResponseEntity.ok(assemblyService.buildByRecurrence(user, resolved));
    }

    @GetMapping("/comparison")
    public ResponseEntity<PeriodComparisonResponse> compareExpenses(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth compareTo,
            @RequestParam(required = false, defaultValue = "false") boolean showTotal,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = resolveUser(userDetails);
        return ResponseEntity.ok(
                assemblyService.buildComparison(user, month, compareTo, showTotal));
    }
}
