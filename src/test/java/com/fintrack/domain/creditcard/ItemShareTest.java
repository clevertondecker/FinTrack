package com.fintrack.domain.creditcard;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;

@DisplayName("ItemShare Entity Tests")
class ItemShareTest {

    private User testUser;
    private Invoice testInvoice;
    private InvoiceItem testInvoiceItem;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.of("Test User", "test@example.com", "password123", Set.of(Role.USER));
        Bank testBank = Bank.of("Test Bank", "1234");
        CreditCard testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
        testInvoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 15));
        testCategory = Category.of("Food", "#FF0000");
        testInvoiceItem = InvoiceItem.of(testInvoice, "Test Item", new BigDecimal("100.00"), testCategory, LocalDate.of(2024, 1, 10));
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create ItemShare with valid parameters successfully")
        void shouldCreateItemShareWithValidParametersSuccessfully() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"), false);

            assertThat(share.getInvoiceItem()).isEqualTo(testInvoiceItem);
            assertThat(share.getUser()).isEqualTo(testUser);
            assertThat(share.getPercentage()).isEqualByComparingTo(new BigDecimal("0.5"));
            assertThat(share.getAmount()).isEqualByComparingTo(new BigDecimal("50.00")); // 50% of 100.00
            assertThat(share.isResponsible()).isFalse();
            assertThat(share.getCreatedAt()).isNotNull();
            assertThat(share.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should create ItemShare with responsible flag")
        void shouldCreateItemShareWithResponsibleFlag() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.3"), new BigDecimal("30.00"), true);

            assertThat(share.isResponsible()).isTrue();
        }

        @Test
        @DisplayName("Should create ItemShare with minimum percentage")
        void shouldCreateItemShareWithMinimumPercentage() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.01"), new BigDecimal("1.00"), false);

            assertThat(share.getPercentage()).isEqualByComparingTo(new BigDecimal("0.01"));
            assertThat(share.getAmount()).isEqualByComparingTo(new BigDecimal("1.00")); // 1% of 100.00
        }

        @Test
        @DisplayName("Should create ItemShare with maximum percentage")
        void shouldCreateItemShareWithMaximumPercentage() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("1.0"), new BigDecimal("100.00"), false);

            assertThat(share.getPercentage()).isEqualByComparingTo(new BigDecimal("1.0"));
            assertThat(share.getAmount()).isEqualByComparingTo(new BigDecimal("100.00")); // 100% of 100.00
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when user is null")
        void shouldThrowExceptionWhenUserIsNull() {
            assertThatThrownBy(() -> ItemShare.of(null, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"), false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("User must not be null.");
        }

        @Test
        @DisplayName("Should throw exception when invoice item is null")
        void shouldThrowExceptionWhenInvoiceItemIsNull() {
            assertThatThrownBy(() -> ItemShare.of(testUser, null, new BigDecimal("0.5"), new BigDecimal("50.00"), false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Invoice item must not be null.");
        }

        @Test
        @DisplayName("Should throw exception when percentage is null")
        void shouldThrowExceptionWhenPercentageIsNull() {
            assertThatThrownBy(() -> ItemShare.of(testUser, testInvoiceItem, null, new BigDecimal("50.00"), false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Percentage must not be null.");
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThatThrownBy(() -> ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Amount must not be null.");
        }

        @Test
        @DisplayName("Should throw exception when percentage exceeds one")
        void shouldThrowExceptionWhenPercentageExceedsOne() {
            assertThatThrownBy(() -> ItemShare.of(testUser, testInvoiceItem, new BigDecimal("1.1"), new BigDecimal("50.00"), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Percentage cannot exceed 1.0 (100%).");
        }

        @Test
        @DisplayName("Should throw exception when percentage is negative")
        void shouldThrowExceptionWhenPercentageIsNegative() {
            assertThatThrownBy(() -> ItemShare.of(testUser, testInvoiceItem, new BigDecimal("-0.1"), new BigDecimal("50.00"), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Percentage must be non-negative.");
        }
    }

    @Nested
    @DisplayName("Update Methods Tests")
    class UpdateMethodsTests {

        private ItemShare share;

        @BeforeEach
        void setUp() {
            share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"), false);
        }

        @Test
        @DisplayName("Should update percentage successfully")
        void shouldUpdatePercentageSuccessfully() {
            BigDecimal newPercentage = new BigDecimal("0.7");
            LocalDateTime beforeUpdate = share.getUpdatedAt();

            share.updatePercentage(newPercentage);

            assertThat(share.getPercentage()).isEqualByComparingTo(newPercentage);
            assertThat(share.getAmount()).isEqualByComparingTo(new BigDecimal("70.00")); // 70% of 100.00
            assertThat(share.getUpdatedAt()).isAfter(beforeUpdate);
        }

        @Test
        @DisplayName("Should update percentage to zero")
        void shouldUpdatePercentageToZero() {
            BigDecimal newPercentage = BigDecimal.ZERO;

            share.updatePercentage(newPercentage);

            assertThat(share.getPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(share.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should update percentage to 100%")
        void shouldUpdatePercentageToHundredPercent() {
            BigDecimal newPercentage = BigDecimal.ONE;

            share.updatePercentage(newPercentage);

            assertThat(share.getPercentage()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(share.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should throw exception when updating percentage to null")
        void shouldThrowExceptionWhenUpdatingPercentageToNull() {
            assertThatThrownBy(() -> share.updatePercentage(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("New percentage must not be null.");
        }

        @Test
        @DisplayName("Should throw exception when updating percentage to negative")
        void shouldThrowExceptionWhenUpdatingPercentageToNegative() {
            BigDecimal newPercentage = new BigDecimal("-0.1");

            assertThatThrownBy(() -> share.updatePercentage(newPercentage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Percentage must be non-negative.");
        }

        @Test
        @DisplayName("Should throw exception when updating percentage to exceed one")
        void shouldThrowExceptionWhenUpdatingPercentageToExceedOne() {
            assertThatThrownBy(() -> share.updatePercentage(new BigDecimal("1.1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Percentage cannot exceed 1.0 (100%).");
        }

        @Test
        @DisplayName("Should set responsible flag successfully")
        void shouldSetResponsibleFlagSuccessfully() {
            LocalDateTime beforeUpdate = share.getUpdatedAt();

            share.setResponsible(true);

            assertThat(share.isResponsible()).isTrue();
            assertThat(share.getUpdatedAt()).isAfter(beforeUpdate);
        }

        @Test
        @DisplayName("Should unset responsible flag successfully")
        void shouldUnsetResponsibleFlagSuccessfully() {
            share.setResponsible(true);
            LocalDateTime beforeUpdate = share.getUpdatedAt();

            share.setResponsible(false);

            assertThat(share.isResponsible()).isFalse();
            assertThat(share.getUpdatedAt()).isAfter(beforeUpdate);
        }
    }

    @Nested
    @DisplayName("Getter Methods Tests")
    class GetterMethodsTests {

        @Test
        @DisplayName("Should return correct values from getters")
        void shouldReturnCorrectValuesFromGetters() {
            BigDecimal percentage = new BigDecimal("0.6");
            BigDecimal amount = new BigDecimal("60.00");
            boolean responsible = true;

            ItemShare share = ItemShare.of(testUser, testInvoiceItem, percentage, amount, responsible);

            assertThat(share.getUser()).isEqualTo(testUser);
            assertThat(share.getInvoiceItem()).isEqualTo(testInvoiceItem);
            assertThat(share.getPercentage()).isEqualTo(percentage);
            assertThat(share.getAmount()).isEqualTo(amount);
            assertThat(share.isResponsible()).isEqualTo(responsible);
            assertThat(share.getCreatedAt()).isNotNull();
            assertThat(share.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return null ID when not persisted")
        void shouldReturnNullIdWhenNotPersisted() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));

            assertThat(share.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));

            assertThat(share).isEqualTo(share);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));

            assertThat(share).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));

            assertThat(share).isNotEqualTo("string");
        }

        @Test
        @DisplayName("Should have same hash code for equal objects")
        void shouldHaveSameHashCodeForEqualObjects() {
            ItemShare share1 = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));
            ItemShare share2 = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));

            assertThat(share1).hasSameHashCodeAs(share2);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return meaningful string representation")
        void shouldReturnMeaningfulStringRepresentation() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"), true);

            String toString = share.toString();

            assertThat(toString).contains("ItemShare{");
            assertThat(toString).contains("user=");
            assertThat(toString).contains("invoiceItem=");
            assertThat(toString).contains("percentage=0.5");
            assertThat(toString).contains("amount=50.00");
            assertThat(toString).contains("responsible=true");
            assertThat(toString).contains("createdAt=");
            assertThat(toString).contains("updatedAt=");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very small percentage values")
        void shouldHandleVerySmallPercentageValues() {
            BigDecimal percentage = new BigDecimal("0.0001");
            BigDecimal amount = new BigDecimal("0.01");

            ItemShare share = ItemShare.of(testUser, testInvoiceItem, percentage, amount);

            assertThat(share.getPercentage()).isEqualTo(percentage);
            assertThat(share.getAmount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("Should handle very large amount values")
        void shouldHandleVeryLargeAmountValues() {
            BigDecimal percentage = new BigDecimal("0.5");
            BigDecimal amount = new BigDecimal("999999.99");

            ItemShare share = ItemShare.of(testUser, testInvoiceItem, percentage, amount);

            assertThat(share.getPercentage()).isEqualTo(percentage);
            assertThat(share.getAmount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("Should handle multiple percentage updates")
        void shouldHandleMultiplePercentageUpdates() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.3"), new BigDecimal("30.00"));

            share.updatePercentage(new BigDecimal("0.5"));
            assertThat(share.getPercentage()).isEqualByComparingTo(new BigDecimal("0.5"));
            assertThat(share.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));

            share.updatePercentage(new BigDecimal("0.8"));
            assertThat(share.getPercentage()).isEqualByComparingTo(new BigDecimal("0.8"));
            assertThat(share.getAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        }
    }
} 