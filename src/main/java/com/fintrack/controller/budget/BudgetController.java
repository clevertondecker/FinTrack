package com.fintrack.controller.budget;

import com.fintrack.application.budget.BudgetService;
import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.User;
import com.fintrack.dto.budget.BudgetResponse;
import com.fintrack.dto.budget.BudgetStatusResponse;
import com.fintrack.dto.budget.CreateBudgetRequest;
import com.fintrack.dto.budget.UpdateBudgetRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final UserService userService;

    public BudgetController(final BudgetService budgetService,
                            final UserService userService) {
        this.budgetService = budgetService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(
            @Valid @RequestBody CreateBudgetRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        BudgetResponse response = budgetService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BudgetStatusResponse>> getBudgets(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        if (month == null) {
            month = YearMonth.now();
        }
        return ResponseEntity.ok(budgetService.getStatusForMonth(user, month));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(budgetService.update(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getCurrentUser(userDetails.getUsername());
        budgetService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
