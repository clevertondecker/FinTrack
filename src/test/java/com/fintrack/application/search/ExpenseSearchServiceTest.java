package com.fintrack.application.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.search.ExpenseSearchRequest;

@DisplayName("ExpenseSearchService Tests")
class ExpenseSearchServiceTest {

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        private final ExpenseSearchService service = new ExpenseSearchService(null);

        @Test
        @DisplayName("Should reject null user")
        void shouldRejectNullUser() {
            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    "test", null, null, null, null, null, null, 0, 20);

            assertThatThrownBy(() -> service.search(null, request))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("User must not be null");
        }

        @Test
        @DisplayName("Should reject null request")
        void shouldRejectNullRequest() {
            User testUser = User.createLocalUser("John", "john@test.com", "pass123", Set.of(Role.USER));

            assertThatThrownBy(() -> service.search(testUser, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Search request must not be null");
        }
    }

    @Nested
    @DisplayName("ExpenseSearchRequest Record Validation")
    class RequestRecordValidation {

        @Test
        @DisplayName("Should normalize negative page to zero")
        void shouldNormalizeNegativePage() {
            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    "test", null, null, null, null, null, null, -5, 20);

            assertThat(request.page()).isZero();
        }

        @Test
        @DisplayName("Should normalize zero size to default (20)")
        void shouldNormalizeZeroSize() {
            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    "test", null, null, null, null, null, null, 0, 0);

            assertThat(request.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should normalize negative size to default (20)")
        void shouldNormalizeNegativeSize() {
            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    "test", null, null, null, null, null, null, 0, -10);

            assertThat(request.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should normalize oversized page size to default (20)")
        void shouldNormalizeOversizedPageSize() {
            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    "test", null, null, null, null, null, null, 0, 100);

            assertThat(request.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should accept max valid page size (50)")
        void shouldAcceptMaxValidPageSize() {
            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    "test", null, null, null, null, null, null, 0, 50);

            assertThat(request.size()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should reject page size just over max (51)")
        void shouldRejectPageSizeJustOverMax() {
            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    "test", null, null, null, null, null, null, 0, 51);

            assertThat(request.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should keep valid page and size")
        void shouldKeepValidPageAndSize() {
            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    "test", null, null, null, null, null, null, 3, 15);

            assertThat(request.page()).isEqualTo(3);
            assertThat(request.size()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should preserve all filter fields")
        void shouldPreserveAllFilterFields() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 3, 31);
            BigDecimal min = new BigDecimal("10.00");
            BigDecimal max = new BigDecimal("500.00");

            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    "ifood", 1L, 2L, from, to, min, max, 2, 15);

            assertThat(request.query()).isEqualTo("ifood");
            assertThat(request.categoryId()).isEqualTo(1L);
            assertThat(request.cardId()).isEqualTo(2L);
            assertThat(request.dateFrom()).isEqualTo(from);
            assertThat(request.dateTo()).isEqualTo(to);
            assertThat(request.amountMin()).isEqualByComparingTo(min);
            assertThat(request.amountMax()).isEqualByComparingTo(max);
            assertThat(request.page()).isEqualTo(2);
            assertThat(request.size()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should allow null optional fields")
        void shouldAllowNullOptionalFields() {
            ExpenseSearchRequest request = new ExpenseSearchRequest(
                    null, null, null, null, null, null, null, 0, 20);

            assertThat(request.query()).isNull();
            assertThat(request.categoryId()).isNull();
            assertThat(request.cardId()).isNull();
            assertThat(request.dateFrom()).isNull();
            assertThat(request.dateTo()).isNull();
            assertThat(request.amountMin()).isNull();
            assertThat(request.amountMax()).isNull();
        }
    }
}
