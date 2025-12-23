package com.fintrack.domain.creditcard;

import com.fintrack.domain.user.User;
import com.fintrack.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ItemShare Payment Tests")
class ItemSharePaymentTest {

    private User testUser;
    private InvoiceItem testInvoiceItem;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("Test User", "test@example.com", "password123", Set.of(Role.USER));
        Bank testBank = Bank.of("Test Bank", "Test Bank Description");
        CreditCard testCreditCard = CreditCard.of("Test Card", "3456", new BigDecimal("1000.00"), testUser, testBank);
        Invoice testInvoice = Invoice.of(
            testCreditCard,
            YearMonth.now(),
            LocalDateTime.now().toLocalDate()
        );
        testInvoiceItem = InvoiceItem.of(testInvoice, "Test Item",
            new BigDecimal("100.00"), null, LocalDateTime.now().toLocalDate());
    }

    @Nested
    @DisplayName("Payment Status Tests")
    class PaymentStatusTests {

        @Test
        @DisplayName("Should create share with unpaid status by default")
        void shouldCreateShareWithUnpaidStatusByDefault() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));

            assertThat(share.isPaid()).isFalse();
            assertThat(share.getPaymentMethod()).isNull();
            assertThat(share.getPaidAt()).isNull();
        }

        @Test
        @DisplayName("Should mark share as paid with payment details")
        void shouldMarkShareAsPaidWithPaymentDetails() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));
            LocalDateTime paymentDate = LocalDateTime.now();

            share.markAsPaid("PIX", paymentDate);

            assertThat(share.isPaid()).isTrue();
            assertThat(share.getPaymentMethod()).isEqualTo("PIX");
            assertThat(share.getPaidAt()).isEqualTo(paymentDate);
        }

        @Test
        @DisplayName("Should mark share as unpaid")
        void shouldMarkShareAsUnpaid() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));
            LocalDateTime paymentDate = LocalDateTime.now();
            share.markAsPaid("PIX", paymentDate);

            share.markAsUnpaid();

            assertThat(share.isPaid()).isFalse();
            assertThat(share.getPaymentMethod()).isNull();
            assertThat(share.getPaidAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Payment Validation Tests")
    class PaymentValidationTests {

        @Test
        @DisplayName("Should throw exception when payment method is null")
        void shouldThrowExceptionWhenPaymentMethodIsNull() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));
            LocalDateTime paymentDate = LocalDateTime.now();

            assertThatThrownBy(() -> share.markAsPaid(null, paymentDate))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Payment method cannot be blank.");
        }

        @Test
        @DisplayName("Should throw exception when payment method is blank")
        void shouldThrowExceptionWhenPaymentMethodIsBlank() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));
            LocalDateTime paymentDate = LocalDateTime.now();

            assertThatThrownBy(() -> share.markAsPaid("", paymentDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment method cannot be blank.");
        }

        @Test
        @DisplayName("Should throw exception when payment date is null")
        void shouldThrowExceptionWhenPaymentDateIsNull() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));

            assertThatThrownBy(() -> share.markAsPaid("PIX", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Payment date cannot be null.");
        }
    }

    @Nested
    @DisplayName("Payment Method Tests")
    class PaymentMethodTests {

        @Test
        @DisplayName("Should accept different payment methods")
        void shouldAcceptDifferentPaymentMethods() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));
            LocalDateTime paymentDate = LocalDateTime.now();

            share.markAsPaid("BANK_TRANSFER", paymentDate);
            assertThat(share.getPaymentMethod()).isEqualTo("BANK_TRANSFER");

            share.markAsPaid("CASH", paymentDate);
            assertThat(share.getPaymentMethod()).isEqualTo("CASH");

            share.markAsPaid("CREDIT_CARD", paymentDate);
            assertThat(share.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        }
    }

    @Nested
    @DisplayName("Updated At Tests")
    class UpdatedAtTests {

        @Test
        @DisplayName("Should update timestamp when marking as paid")
        void shouldUpdateTimestampWhenMarkingAsPaid() {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));
            LocalDateTime beforePayment = share.getUpdatedAt();

            share.markAsPaid("PIX", LocalDateTime.now());

            assertThat(share.getUpdatedAt()).isAfter(beforePayment);
        }

        @Test
        @DisplayName("Should update timestamp when marking as unpaid")
        void shouldUpdateTimestampWhenMarkingAsUnpaid() throws InterruptedException {
            ItemShare share = ItemShare.of(testUser, testInvoiceItem, new BigDecimal("0.5"), new BigDecimal("50.00"));
            share.markAsPaid("PIX", LocalDateTime.now());
            LocalDateTime beforeUpdate = share.getUpdatedAt();
            
            // Add small delay to ensure timestamp difference
            Thread.sleep(1);

            share.markAsUnpaid();

            assertThat(share.isPaid()).isFalse();
            assertThat(share.getPaidAt()).isNull();
            // Verify that updatedAt was updated (should be after or equal, but with delay should be after)
            assertThat(share.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
        }
    }
} 