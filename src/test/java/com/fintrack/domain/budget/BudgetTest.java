package com.fintrack.domain.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;

@DisplayName("Budget Domain Entity")
class BudgetTest {

    private User owner;
    private Category category;

    @BeforeEach
    void setUp() {
        owner = User.createLocalUser("John", "john@test.com", "pass123", Set.of(Role.USER));
        category = Category.of("Food", "#FF0000");
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create monthly budget with valid parameters")
        void shouldCreateMonthlyBudget() {
            YearMonth month = YearMonth.of(2026, 3);
            Budget budget = Budget.of(owner, category, new BigDecimal("1000.00"), month);

            assertThat(budget.getOwner()).isEqualTo(owner);
            assertThat(budget.getCategory()).isEqualTo(category);
            assertThat(budget.getLimitAmount()).isEqualByComparingTo("1000.00");
            assertThat(budget.getMonth()).isEqualTo(month);
            assertThat(budget.isRecurring()).isFalse();
            assertThat(budget.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should create recurring budget without month")
        void shouldCreateRecurringBudget() {
            Budget budget = Budget.recurring(owner, category, new BigDecimal("500.00"));

            assertThat(budget.getMonth()).isNull();
            assertThat(budget.isRecurring()).isTrue();
            assertThat(budget.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should create general budget without category")
        void shouldCreateGeneralBudget() {
            Budget budget = Budget.of(owner, null, new BigDecimal("2000.00"), YearMonth.now());

            assertThat(budget.getCategory()).isNull();
            assertThat(budget.getLimitAmount()).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("Should throw when owner is null")
        void shouldThrowWhenOwnerIsNull() {
            assertThatThrownBy(() -> Budget.of(null, category, BigDecimal.TEN, YearMonth.now()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Owner must not be null");
        }

        @Test
        @DisplayName("Should throw when limit is null")
        void shouldThrowWhenLimitIsNull() {
            assertThatThrownBy(() -> Budget.of(owner, category, null, YearMonth.now()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Limit amount must not be null");
        }

        @Test
        @DisplayName("Should throw when limit is zero")
        void shouldThrowWhenLimitIsZero() {
            assertThatThrownBy(() -> Budget.of(owner, category, BigDecimal.ZERO, YearMonth.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Limit amount must be positive");
        }

        @Test
        @DisplayName("Should throw when limit is negative")
        void shouldThrowWhenLimitIsNegative() {
            assertThatThrownBy(() -> Budget.of(owner, category, new BigDecimal("-100"), YearMonth.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Limit amount must be positive");
        }
    }

    @Nested
    @DisplayName("Update Limit Tests")
    class UpdateLimitTests {

        @Test
        @DisplayName("Should update limit successfully")
        void shouldUpdateLimit() {
            Budget budget = Budget.of(owner, category, new BigDecimal("1000.00"), YearMonth.now());
            budget.updateLimit(new BigDecimal("1500.00"));

            assertThat(budget.getLimitAmount()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("Should throw when new limit is null")
        void shouldThrowWhenNewLimitIsNull() {
            Budget budget = Budget.of(owner, category, new BigDecimal("1000.00"), YearMonth.now());
            assertThatThrownBy(() -> budget.updateLimit(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should throw when new limit is zero")
        void shouldThrowWhenNewLimitIsZero() {
            Budget budget = Budget.of(owner, category, new BigDecimal("1000.00"), YearMonth.now());
            assertThatThrownBy(() -> budget.updateLimit(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Should deactivate budget")
        void shouldDeactivateBudget() {
            Budget budget = Budget.of(owner, category, new BigDecimal("1000.00"), YearMonth.now());
            budget.deactivate();

            assertThat(budget.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should reactivate budget")
        void shouldReactivateBudget() {
            Budget budget = Budget.of(owner, category, new BigDecimal("1000.00"), YearMonth.now());
            budget.deactivate();
            budget.activate();

            assertThat(budget.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same id")
        void shouldBeEqualWhenSameId() {
            Budget b1 = Budget.of(owner, category, new BigDecimal("1000.00"), YearMonth.now());
            Budget b2 = Budget.of(owner, category, new BigDecimal("2000.00"), YearMonth.now());

            org.springframework.test.util.ReflectionTestUtils.setField(b1, "id", 1L);
            org.springframework.test.util.ReflectionTestUtils.setField(b2, "id", 1L);

            assertThat(b1).isEqualTo(b2);
            assertThat(b1.hashCode()).isEqualTo(b2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different id")
        void shouldNotBeEqualWhenDifferentId() {
            Budget b1 = Budget.of(owner, category, new BigDecimal("1000.00"), YearMonth.now());
            Budget b2 = Budget.of(owner, category, new BigDecimal("1000.00"), YearMonth.now());

            org.springframework.test.util.ReflectionTestUtils.setField(b1, "id", 1L);
            org.springframework.test.util.ReflectionTestUtils.setField(b2, "id", 2L);

            assertThat(b1).isNotEqualTo(b2);
        }
    }
}
