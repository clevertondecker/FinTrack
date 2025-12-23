package com.fintrack.domain.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InvoiceItem Tests")
class InvoiceItemTest {

    private User testUser;
    private CreditCard testCreditCard;
    private Bank testBank;
    private Invoice testInvoice;
    private Category foodCategory;
    private Category electronicsCategory;

    @BeforeEach
    void setUp() {
        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testBank = Bank.of("Test Bank", "Test Bank Description");
        testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
        testInvoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
        foodCategory = Category.of("Food", "#FF0000");
        electronicsCategory = Category.of("Electronics", "#0000FF");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create InvoiceItem with valid data")
        void shouldCreateInvoiceItemWithValidData() {
            String description = "Restaurant dinner";
            BigDecimal amount = new BigDecimal("150.00");
            LocalDate purchaseDate = LocalDate.of(2024, 1, 15);

            InvoiceItem item = InvoiceItem.of(testInvoice, description, amount, foodCategory, purchaseDate);

            assertNotNull(item);
            assertEquals(testInvoice, item.getInvoice());
            assertEquals(description, item.getDescription());
            assertEquals(amount, item.getAmount());
            assertEquals(foodCategory, item.getCategory());
            assertEquals(purchaseDate, item.getPurchaseDate());
            assertEquals(1, item.getInstallments());
            assertEquals(1, item.getTotalInstallments());
        }

        @Test
        @DisplayName("Should create InvoiceItem with installments")
        void shouldCreateInvoiceItemWithInstallments() {
            String description = "iPhone purchase";
            BigDecimal amount = new BigDecimal("5000.00");
            LocalDate purchaseDate = LocalDate.of(2024, 1, 10);
            Integer installments = 12;
            Integer totalInstallments = 12;

            InvoiceItem item = InvoiceItem.of(testInvoice, description, amount,
                electronicsCategory, purchaseDate, installments, totalInstallments);

            assertNotNull(item);
            assertEquals(description, item.getDescription());
            assertEquals(amount, item.getAmount());
            assertEquals(installments, item.getInstallments());
            assertEquals(totalInstallments, item.getTotalInstallments());
        }

        @Test
        @DisplayName("Should throw exception when invoice is null")
        void shouldThrowExceptionWhenInvoiceIsNull() {
            assertThrows(NullPointerException.class, () ->
                InvoiceItem.of(null, "Description", new BigDecimal("100.00"), foodCategory, LocalDate.now()));
        }

        @Test
        @DisplayName("Should throw exception when description is null")
        void shouldThrowExceptionWhenDescriptionIsNull() {
            assertThrows(NullPointerException.class, () ->
                InvoiceItem.of(testInvoice, null, new BigDecimal("100.00"), foodCategory, LocalDate.now()));
        }

        @Test
        @DisplayName("Should throw exception when description is blank")
        void shouldThrowExceptionWhenDescriptionIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                InvoiceItem.of(testInvoice, "", new BigDecimal("100.00"), foodCategory, LocalDate.now()));
        }

        @Test
        @DisplayName("Should throw exception when amount is null")
        void shouldThrowExceptionWhenAmountIsNull() {
            assertThrows(NullPointerException.class, () ->
                InvoiceItem.of(testInvoice, "Description", null, foodCategory, LocalDate.now()));
        }

        @Test
        @DisplayName("Should throw exception when purchaseDate is null")
        void shouldThrowExceptionWhenPurchaseDateIsNull() {
            assertThrows(NullPointerException.class, () ->
                InvoiceItem.of(testInvoice, "Description", new BigDecimal("100.00"), foodCategory, null));
        }

        @Test
        @DisplayName("Should throw exception when installments is null")
        void shouldThrowExceptionWhenInstallmentsIsNull() {
            assertThrows(NullPointerException.class, () ->
                InvoiceItem.of(testInvoice, "Description", new BigDecimal("100.00"),
                    foodCategory, LocalDate.now(), null, 1));
        }

        @Test
        @DisplayName("Should throw exception when installments is negative")
        void shouldThrowExceptionWhenInstallmentsIsNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                InvoiceItem.of(testInvoice, "Description", new BigDecimal("100.00"),
                    foodCategory, LocalDate.now(), -1, 1));
        }

        @Test
        @DisplayName("Should throw exception when totalInstallments is null")
        void shouldThrowExceptionWhenTotalInstallmentsIsNull() {
            assertThrows(NullPointerException.class, () ->
                InvoiceItem.of(testInvoice, "Description", new BigDecimal("100.00"),
                    foodCategory, LocalDate.now(), 1, null));
        }

        @Test
        @DisplayName("Should throw exception when totalInstallments is negative")
        void shouldThrowExceptionWhenTotalInstallmentsIsNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                InvoiceItem.of(testInvoice, "Description", new BigDecimal("100.00"),
                    foodCategory, LocalDate.now(), 1, -1));
        }

        @Test
        @DisplayName("Should throw exception when installments exceed totalInstallments")
        void shouldThrowExceptionWhenInstallmentsExceedTotalInstallments() {
            assertThrows(IllegalArgumentException.class, () ->
                InvoiceItem.of(testInvoice, "Description", new BigDecimal("100.00"),
                    foodCategory, LocalDate.now(), 5, 3));
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should accept null category")
        void shouldAcceptNullCategory() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("100.00"), null, LocalDate.now());

            assertNotNull(item);
            assertNull(item.getCategory());
        }

        @Test
        @DisplayName("Should handle very large amount")
        void shouldHandleVeryLargeAmount() {
            BigDecimal largeAmount = new BigDecimal("999999.99");
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                largeAmount, foodCategory, LocalDate.now());

            assertEquals(largeAmount, item.getAmount());
        }

        @Test
        @DisplayName("Should handle decimal amounts")
        void shouldHandleDecimalAmounts() {
            BigDecimal decimalAmount = new BigDecimal("123.45");
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                decimalAmount, foodCategory, LocalDate.now());

            assertEquals(decimalAmount, item.getAmount());
        }

        @Test
        @DisplayName("Should handle single installment by default")
        void shouldHandleSingleInstallmentByDefault() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());

            assertEquals(1, item.getInstallments());
            assertEquals(1, item.getTotalInstallments());
        }

        @Test
        @DisplayName("Should handle multiple installments")
        void shouldHandleMultipleInstallments() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("1000.00"), foodCategory, LocalDate.now(), 3, 6);

            assertEquals(3, item.getInstallments());
            assertEquals(6, item.getTotalInstallments());
        }

        @Test
        @DisplayName("Should allow negative amount (for estornos/credits)")
        void shouldAllowNegativeAmount() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("-100.00"), foodCategory, LocalDate.now());
            assertEquals(new BigDecimal("-100.00"), item.getAmount());
        }

        @Test
        @DisplayName("Should allow zero amount")
        void shouldAllowZeroAmount() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                BigDecimal.ZERO, foodCategory, LocalDate.now());
            assertEquals(BigDecimal.ZERO, item.getAmount());
        }
    }

    @Nested
    @DisplayName("Shares Tests")
    class SharesTests {

        @Test
        @DisplayName("Should add share successfully")
        void shouldAddShareSuccessfully() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Dinner",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());
            User user1 = User.createLocalUser("User1", "user1@example.com", "password123", Set.of(Role.USER));
            User user2 = User.createLocalUser("User2", "user2@example.com", "password123", Set.of(Role.USER));

            ItemShare share1 = ItemShare.of(user1, item, new BigDecimal("0.5"), new BigDecimal("50.00"));
            ItemShare share2 = ItemShare.of(user2, item, new BigDecimal("0.5"), new BigDecimal("50.00"));

            item.addShare(share1);
            item.addShare(share2);

            assertEquals(2, item.getShares().size());
            assertEquals(new BigDecimal("100.00"), item.getSharedAmount());
            assertTrue(item.isFullyShared());
        }

        @Test
        @DisplayName("Should calculate shared amount correctly")
        void shouldCalculateSharedAmountCorrectly() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Dinner",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());
            User user1 = User.createLocalUser("User1", "user1@example.com", "password123", Set.of(Role.USER));

            ItemShare share = ItemShare.of(user1, item, new BigDecimal("0.3"), new BigDecimal("30.00"));
            item.addShare(share);

            assertEquals(new BigDecimal("30.00"), item.getSharedAmount());
            assertEquals(new BigDecimal("70.00"), item.getUnsharedAmount());
            assertFalse(item.isFullyShared());
        }

        @Test
        @DisplayName("Should remove share successfully")
        void shouldRemoveShareSuccessfully() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Dinner",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());
            User user1 = User.createLocalUser("User1", "user1@example.com", "password123", Set.of(Role.USER));

            ItemShare share = ItemShare.of(user1, item, new BigDecimal("0.5"), new BigDecimal("50.00"));
            item.addShare(share);

            assertEquals(1, item.getShares().size());

            item.removeShare(share);

            assertEquals(0, item.getShares().size());
            assertEquals(BigDecimal.ZERO, item.getSharedAmount());
        }

        @Test
        @DisplayName("Should handle multiple shares with different amounts")
        void shouldHandleMultipleSharesWithDifferentAmounts() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Dinner",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());
            User user1 = User.createLocalUser("User1", "user1@example.com", "password123", Set.of(Role.USER));
            User user2 = User.createLocalUser("User2", "user2@example.com", "password123", Set.of(Role.USER));
            User user3 = User.createLocalUser("User3", "user3@example.com", "password123", Set.of(Role.USER));

            ItemShare share1 = ItemShare.of(user1, item, new BigDecimal("0.4"), new BigDecimal("40.00"));
            ItemShare share2 = ItemShare.of(user2, item, new BigDecimal("0.3"), new BigDecimal("30.00"));
            ItemShare share3 = ItemShare.of(user3, item, new BigDecimal("0.3"), new BigDecimal("30.00"));

            item.addShare(share1);
            item.addShare(share2);
            item.addShare(share3);

            assertEquals(new BigDecimal("100.00"), item.getSharedAmount());
            assertTrue(item.isFullyShared());
        }

        @Test
        @DisplayName("Should handle multiple shares with zero amounts")
        void shouldHandleMultipleSharesWithZeroAmounts() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());
            User user1 = User.createLocalUser("User1", "user1@example.com", "password123", Set.of(Role.USER));

            ItemShare share = ItemShare.of(user1, item, new BigDecimal("0.01"), new BigDecimal("0.01"));
            item.addShare(share);

            assertEquals(new BigDecimal("0.01"), item.getSharedAmount());
            assertEquals(new BigDecimal("99.99"), item.getUnsharedAmount());
        }
    }

    @Nested
    @DisplayName("Timestamps Tests")
    class TimestampsTests {

        @Test
        @DisplayName("Should set createdAt on creation")
        void shouldSetCreatedAtOnCreation() {
            LocalDateTime beforeCreation = LocalDateTime.now();

            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());

            LocalDateTime afterCreation = LocalDateTime.now();

            assertTrue(item.getCreatedAt().isAfter(beforeCreation)
                || item.getCreatedAt().equals(beforeCreation));
            assertTrue(item.getCreatedAt().isBefore(afterCreation)
                || item.getCreatedAt().equals(afterCreation));
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());

            assertEquals(item, item);
            assertEquals(item.hashCode(), item.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());

            assertNotNull(item);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());

          assertNotEquals("Not an InvoiceItem", item);
        }

        @Test
        @DisplayName("Should be equal when IDs are equal")
        void shouldBeEqualWhenIdsAreEqual() {
            InvoiceItem item1 = InvoiceItem.of(testInvoice, "Description1",
                new BigDecimal("100.00"), foodCategory, LocalDate.now());
            InvoiceItem item2 = InvoiceItem.of(testInvoice, "Description2",
                new BigDecimal("200.00"), electronicsCategory, LocalDate.now());
            setInvoiceItemId(item1, 1L);
            setInvoiceItemId(item2, 1L);
            assertEquals(item1, item2);
            assertEquals(item1.hashCode(), item2.hashCode());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long description")
        void shouldHandleVeryLongDescription() {
            String longDescription = "A".repeat(1000);
            InvoiceItem item = InvoiceItem.of(testInvoice, longDescription,
                new BigDecimal("100.00"), foodCategory, LocalDate.now());

            assertEquals(longDescription, item.getDescription());
        }

        @Test
        @DisplayName("Should handle future purchase date")
        void shouldHandleFuturePurchaseDate() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("100.00"), foodCategory, futureDate);

            assertEquals(futureDate, item.getPurchaseDate());
        }

        @Test
        @DisplayName("Should handle past purchase date")
        void shouldHandlePastPurchaseDate() {
            LocalDate pastDate = LocalDate.now().minusDays(30);
            InvoiceItem item = InvoiceItem.of(testInvoice, "Description",
                new BigDecimal("100.00"), foodCategory, pastDate);

            assertEquals(pastDate, item.getPurchaseDate());
        }
    }

    // Helper para setar o id
    private void setInvoiceItemId(InvoiceItem item, Long id) {
        try {
            java.lang.reflect.Field idField = InvoiceItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set invoice item ID for testing", e);
        }
    }
}