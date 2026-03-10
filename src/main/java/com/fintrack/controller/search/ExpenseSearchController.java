package com.fintrack.controller.search;

import com.fintrack.application.search.ExpenseSearchService;
import com.fintrack.application.user.UserService;
import com.fintrack.domain.user.User;
import com.fintrack.dto.search.ExpenseSearchRequest;
import com.fintrack.dto.search.ExpenseSearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses/search")
public class ExpenseSearchController {

    private final ExpenseSearchService searchService;
    private final UserService userService;

    public ExpenseSearchController(final ExpenseSearchService searchService,
                                   final UserService userService) {
        this.searchService = searchService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ExpenseSearchResponse> search(
            final ExpenseSearchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(searchService.search(user, request));
    }
}
