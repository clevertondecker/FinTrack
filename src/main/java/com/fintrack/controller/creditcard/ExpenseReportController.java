package com.fintrack.controller.creditcard;

import com.fintrack.application.creditcard.ExpenseReportServiceImpl;
import com.fintrack.application.creditcard.InvoiceService;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.*;
import com.fintrack.dto.user.UserResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    private final ExpenseReportServiceImpl expenseReportService;
    private final InvoiceService invoiceService;

    /**
     * Constructs a new ExpenseReportController.
     *
     * @param expenseReportService the expense report service. Must not be null.
     * @param invoiceService the invoice service. Must not be null.
     */
    public ExpenseReportController(
            final ExpenseReportServiceImpl expenseReportService,
            final InvoiceService invoiceService) {
        this.expenseReportService = expenseReportService;
        this.invoiceService = invoiceService;
    }

    /**
     * Gets an expense report by category for the current user.
     *
     * @param month the month to generate the report for (format: yyyy-MM). Optional, defaults to current month.
     * @param categoryId optional category ID to filter by a specific category.
     * @param userDetails the authenticated user details.
     * @return a response with expenses grouped by category.
     */
    @GetMapping("/by-category")
    public ResponseEntity<ExpenseReportResponse> getExpensesByCategory(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Default to current month if not provided
        if (month == null) {
            month = YearMonth.now();
        }

        // Get expenses by category
        Map<Category, BigDecimal> expensesByCategory = expenseReportService.getExpensesByCategory(user, month);

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
            BigDecimal totalAmount = entry.getValue();

            // Get detailed expense information for this category
            List<ExpenseDetailResponse> details = expenseReportService.getExpenseDetails(user, month, category);
            
            ExpenseByCategoryResponse expenseResponse = new ExpenseByCategoryResponse(
                    CategoryResponse.from(category),
                    totalAmount,
                    details.size(),
                    details
            );
            categoryExpenses.add(expenseResponse);
        }

        // Sort by total amount descending
        categoryExpenses.sort((a, b) -> b.totalAmount().compareTo(a.totalAmount()));

        BigDecimal totalAmount = expenseReportService.getTotalExpenses(user, month);

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

        Optional<User> userOpt = invoiceService.findUserByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

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

