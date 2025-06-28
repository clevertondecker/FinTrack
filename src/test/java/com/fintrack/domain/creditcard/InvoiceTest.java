package com.fintrack.domain.creditcard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

@DisplayName("Invoice Tests")
class InvoiceTest {

    private User testUser;
    private CreditCard testCreditCard;
    private Bank testBank;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.of("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testBank = Bank.of("Test Bank", "Test Bank Description");
        testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
        testCategory = Category.of("Food", "#FF0000");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create Invoice with valid data")
        void shouldCreateInvoiceWithValidData() {
            YearMonth month = YearMonth.of(2024, 1);
            LocalDate dueDate = LocalDate.of(2024, 2, 10);

            Invoice invoice = Invoice.of(testCreditCard, month, dueDate);

            assertNotNull(invoice);
            assertEquals(testCreditCard, invoice.getCreditCard());
            assertEquals(month, invoice.getMonth());
            assertEquals(dueDate, invoice.getDueDate());
            assertEquals(BigDecimal.ZERO, invoice.getTotalAmount());
            assertEquals(BigDecimal.ZERO, invoice.getPaidAmount());
            assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
            assertTrue(invoice.getItems().isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when creditCard is null")
        void shouldThrowExceptionWhenCreditCardIsNull() {
            assertThrows(NullPointerException.class, () ->
                Invoice.of(null, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10)));
        }

        @Test
        @DisplayName("Should throw exception when month is null")
        void shouldThrowExceptionWhenMonthIsNull() {
            assertThrows(NullPointerException.class, () ->
                Invoice.of(testCreditCard, null, LocalDate.of(2024, 2, 10)));
        }

        @Test
        @DisplayName("Should throw exception when dueDate is null")
        void shouldThrowExceptionWhenDueDateIsNull() {
            assertThrows(NullPointerException.class, () ->
                Invoice.of(testCreditCard, YearMonth.of(2024, 1), null));
        }
    }

    @Nested
    @DisplayName("Payment Tests")
    class PaymentTests {

        @Test
        @DisplayName("Should record payment successfully")
        void shouldRecordPaymentSuccessfully() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);

            BigDecimal paymentAmount = new BigDecimal("50.00");
            invoice.recordPayment(paymentAmount);

            assertEquals(paymentAmount, invoice.getPaidAmount());
            assertEquals(InvoiceStatus.PARTIAL, invoice.getStatus());
        }

        @Test
        @DisplayName("Should record full payment")
        void shouldRecordFullPayment() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);

            invoice.recordPayment(new BigDecimal("100.00"));

            assertEquals(new BigDecimal("100.00"), invoice.getPaidAmount());
            assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when payment amount is null")
        void shouldThrowExceptionWhenPaymentAmountIsNull() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            
            assertThrows(NullPointerException.class, () -> invoice.recordPayment(null));
        }

        @Test
        @DisplayName("Should throw exception when payment amount is negative")
        void shouldThrowExceptionWhenPaymentAmountIsNegative() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            
            assertThrows(IllegalArgumentException.class, () -> 
                invoice.recordPayment(new BigDecimal("-50.00")));
        }

        @Test
        @DisplayName("Should throw exception when payment amount exceeds total")
        void shouldThrowExceptionWhenPaymentAmountExceedsTotal() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            assertThrows(IllegalArgumentException.class, () -> 
                invoice.recordPayment(new BigDecimal("150.00")));
        }

        @Test
        @DisplayName("Should calculate remaining amount correctly")
        void shouldCalculateRemainingAmountCorrectly() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            invoice.recordPayment(new BigDecimal("30.00"));
            
            assertEquals(new BigDecimal("70.00"), invoice.getRemainingAmount());
        }
    }

    @Nested
    @DisplayName("Status Tests")
    class StatusTests {

        @Test
        @DisplayName("Should update status to PAID when fully paid")
        void shouldUpdateStatusToPaidWhenFullyPaid() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            invoice.recordPayment(new BigDecimal("100.00"));
            
            assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        }

        @Test
        @DisplayName("Should update status to PARTIAL when partially paid")
        void shouldUpdateStatusToPartialWhenPartiallyPaid() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            invoice.recordPayment(new BigDecimal("50.00"));
            
            assertEquals(InvoiceStatus.PARTIAL, invoice.getStatus());
        }

        @Test
        @DisplayName("Should update status to OVERDUE when past due date")
        void shouldUpdateStatusToOverdueWhenPastDueDate() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            invoice.updateStatus();
            
            assertEquals(InvoiceStatus.OVERDUE, invoice.getStatus());
        }

        @Test
        @DisplayName("Should keep status as OPEN when not paid and not overdue")
        void shouldKeepStatusAsOpenWhenNotPaidAndNotOverdue() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.now().plusDays(30));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            invoice.updateStatus();
            
            assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
        }

        @Test
        @DisplayName("Should update status to CLOSED when zero amount and past due date")
        void shouldUpdateStatusToClosedWhenZeroAmountAndPastDueDate() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 10));
            // No items added, so totalAmount is zero
            
            invoice.updateStatus();
            
            assertEquals(InvoiceStatus.CLOSED, invoice.getStatus());
        }

        @Test
        @DisplayName("Should keep status as OPEN when zero amount and not past due date")
        void shouldKeepStatusAsOpenWhenZeroAmountAndNotPastDueDate() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.now().plusDays(30));
            // No items added, so totalAmount is zero
            
            invoice.updateStatus();
            
            assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
        }

        @Test
        @DisplayName("Should update status to CLOSED when all items removed and past due date")
        void shouldUpdateStatusToClosedWhenAllItemsRemovedAndPastDueDate() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            // Remove the item, making totalAmount zero
            invoice.removeItem(item);
            
            assertEquals(InvoiceStatus.CLOSED, invoice.getStatus());
        }

        @Test
        @DisplayName("Should calculate current status correctly for zero amount past due")
        void shouldCalculateCurrentStatusCorrectlyForZeroAmountPastDue() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 10));
            // No items added, so totalAmount is zero
            
            InvoiceStatus calculatedStatus = invoice.calculateCurrentStatus();
            
            assertEquals(InvoiceStatus.CLOSED, calculatedStatus);
        }

        @Test
        @DisplayName("Should calculate current status correctly for zero amount not past due")
        void shouldCalculateCurrentStatusCorrectlyForZeroAmountNotPastDue() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.now().plusDays(30));
            // No items added, so totalAmount is zero
            
            InvoiceStatus calculatedStatus = invoice.calculateCurrentStatus();
            
            assertEquals(InvoiceStatus.OPEN, calculatedStatus);
        }

        @Test
        @DisplayName("Should change status from CLOSED to OVERDUE when adding item to closed invoice")
        void shouldChangeStatusFromClosedToOverdueWhenAddingItemToClosedInvoice() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 10));
            // No items added, so totalAmount is zero and past due date = CLOSED
            invoice.updateStatus(); // Força o cálculo do status
            
            assertEquals(InvoiceStatus.CLOSED, invoice.getStatus());
            
            // Add an item - should recalculate status
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            // Should now be OVERDUE because has amount and is past due date
            assertEquals(InvoiceStatus.OVERDUE, invoice.getStatus());
        }

        @Test
        @DisplayName("Should change status from CLOSED to OPEN when adding item to closed invoice not past due")
        void shouldChangeStatusFromClosedToOpenWhenAddingItemToClosedInvoiceNotPastDue() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.now().plusDays(30));
            // No items added, so totalAmount is zero and not past due date = OPEN
            
            assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
            
            // Add an item - should recalculate status
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            // Should now be OPEN because has amount and is not past due date
            assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
        }

        @Test
        @DisplayName("Should return to CLOSED when removing all items from overdue invoice")
        void shouldReturnToClosedWhenRemovingAllItemsFromOverdueInvoice() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            // Should be OVERDUE because has amount and is past due date
            assertEquals(InvoiceStatus.OVERDUE, invoice.getStatus());
            
            // Remove the item - should recalculate status
            invoice.removeItem(item);
            
            // Should now be CLOSED because no amount and is past due date
            assertEquals(InvoiceStatus.CLOSED, invoice.getStatus());
        }

        @Test
        @DisplayName("Should allow adding items to closed invoice and recalculate status")
        void shouldAllowAddingItemsToClosedInvoiceAndRecalculateStatus() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 10));
            invoice.updateStatus(); // Force status calculation
            
            // Should be CLOSED because no items and past due date
            assertEquals(InvoiceStatus.CLOSED, invoice.getStatus());
            
            // Add an item - should be allowed and status should change
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            // Should now be OVERDUE because has amount and is past due date
            assertEquals(InvoiceStatus.OVERDUE, invoice.getStatus());
            assertEquals(new BigDecimal("100.00"), invoice.getTotalAmount());
        }

        @Test
        @DisplayName("Should allow payments in closed invoices with zero amount")
        void shouldAllowPaymentsInClosedInvoicesWithZeroAmount() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 10));
            invoice.updateStatus(); // Force status calculation
            
            // Should be CLOSED because no items and past due date
            assertEquals(InvoiceStatus.CLOSED, invoice.getStatus());
            assertEquals(BigDecimal.ZERO, invoice.getTotalAmount());
            
            // Should allow payment even with zero total amount
            invoice.recordPayment(new BigDecimal("50.00"));
            
            // Should now have paid amount
            assertEquals(new BigDecimal("50.00"), invoice.getPaidAmount());
        }
    }

    @Nested
    @DisplayName("Items Tests")
    class ItemsTests {

        @Test
        @DisplayName("Should add item successfully")
        void shouldAddItemSuccessfully() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            
            invoice.addItem(item);
            
            assertEquals(1, invoice.getItems().size());
            assertEquals(new BigDecimal("100.00"), invoice.getTotalAmount());
            assertEquals(invoice, item.getInvoice());
        }

        @Test
        @DisplayName("Should add multiple items")
        void shouldAddMultipleItems() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item1 = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            InvoiceItem item2 = InvoiceItem.of(invoice, "Lunch", new BigDecimal("50.00"), testCategory, LocalDate.now());
            
            invoice.addItem(item1);
            invoice.addItem(item2);
            
            assertEquals(2, invoice.getItems().size());
            assertEquals(new BigDecimal("150.00"), invoice.getTotalAmount());
        }

        @Test
        @DisplayName("Should remove item successfully")
        void shouldRemoveItemSuccessfully() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            invoice.removeItem(item);
            
            assertEquals(0, invoice.getItems().size());
            assertEquals(BigDecimal.ZERO, invoice.getTotalAmount());
            assertEquals(null, item.getInvoice());
        }

        @Test
        @DisplayName("Should recalculate total when items are added")
        void shouldRecalculateTotalWhenItemsAreAdded() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item1 = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            InvoiceItem item2 = InvoiceItem.of(invoice, "Lunch", new BigDecimal("50.00"), testCategory, LocalDate.now());
            InvoiceItem item3 = InvoiceItem.of(invoice, "Breakfast", new BigDecimal("25.00"), testCategory, LocalDate.now());
            
            invoice.addItem(item1);
            assertEquals(new BigDecimal("100.00"), invoice.getTotalAmount());
            
            invoice.addItem(item2);
            assertEquals(new BigDecimal("150.00"), invoice.getTotalAmount());
            
            invoice.addItem(item3);
            assertEquals(new BigDecimal("175.00"), invoice.getTotalAmount());
        }

        @Test
        @DisplayName("Should recalculate total when items are removed")
        void shouldRecalculateTotalWhenItemsAreRemoved() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item1 = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            InvoiceItem item2 = InvoiceItem.of(invoice, "Lunch", new BigDecimal("50.00"), testCategory, LocalDate.now());
            InvoiceItem item3 = InvoiceItem.of(invoice, "Breakfast", new BigDecimal("25.00"), testCategory, LocalDate.now());
            invoice.addItem(item1);
            invoice.addItem(item2);
            invoice.addItem(item3);
            assertEquals(new BigDecimal("175.00"), invoice.getTotalAmount());
            invoice.removeItem(item2); // Remove Lunch (50)
            assertEquals(new BigDecimal("75.00"), invoice.getTotalAmount());
            invoice.removeItem(item1); // Remove Dinner (100)
            assertEquals(new BigDecimal("25.00"), invoice.getTotalAmount());
            invoice.removeItem(item3); // Remove Breakfast (25)
            assertEquals(BigDecimal.ZERO, invoice.getTotalAmount());
        }

        @Test
        @DisplayName("Should throw exception when adding null item")
        void shouldThrowExceptionWhenAddingNullItem() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            
            assertThrows(NullPointerException.class, () -> invoice.addItem(null));
        }

        @Test
        @DisplayName("Should throw exception when removing null item")
        void shouldThrowExceptionWhenRemovingNullItem() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            
            assertThrows(NullPointerException.class, () -> invoice.removeItem(null));
        }
    }

    @Nested
    @DisplayName("Timestamps Tests")
    class TimestampsTests {

        @Test
        @DisplayName("Should set createdAt on creation")
        void shouldSetCreatedAtOnCreation() {
            LocalDateTime beforeCreation = LocalDateTime.now();
            
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            
            LocalDateTime afterCreation = LocalDateTime.now();

            assertTrue(invoice.getCreatedAt().isAfter(beforeCreation) || 
                      invoice.getCreatedAt().equals(beforeCreation));
            assertTrue(invoice.getCreatedAt().isBefore(afterCreation) || 
                      invoice.getCreatedAt().equals(afterCreation));
        }

        @Test
        @DisplayName("Should update updatedAt when adding item")
        void shouldUpdateUpdatedAtWhenAddingItem() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            LocalDateTime beforeUpdate = invoice.getUpdatedAt();
            
            // Wait a bit to ensure time difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            assertTrue(invoice.getUpdatedAt().isAfter(beforeUpdate));
        }

        @Test
        @DisplayName("Should update updatedAt when recording payment")
        void shouldUpdateUpdatedAtWhenRecordingPayment() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            LocalDateTime beforeUpdate = invoice.getUpdatedAt();
            
            // Wait a bit to ensure time difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            invoice.recordPayment(new BigDecimal("50.00"));
            
            assertTrue(invoice.getUpdatedAt().isAfter(beforeUpdate));
        }

        @Test
        @DisplayName("Should have same created and updated timestamps initially")
        void shouldHaveSameCreatedAndUpdatedTimestampsInitially() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            long diff = Math.abs(java.time.Duration.between(invoice.getCreatedAt(), invoice.getUpdatedAt()).toMillis());
            assertTrue(diff < 10, "createdAt e updatedAt devem ser praticamente iguais");
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            
            assertEquals(invoice, invoice);
            assertEquals(invoice.hashCode(), invoice.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            
            assertFalse(invoice.equals(null));
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            
            assertFalse(invoice.equals("Not an Invoice"));
        }

        @Test
        @DisplayName("Should be equal when IDs are equal")
        void shouldBeEqualWhenIdsAreEqual() {
            Invoice invoice1 = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            Invoice invoice2 = Invoice.of(testCreditCard, YearMonth.of(2024, 2), LocalDate.of(2024, 3, 10));
            setInvoiceId(invoice1, 1L);
            setInvoiceId(invoice2, 1L);
            assertEquals(invoice1, invoice2);
            assertEquals(invoice1.hashCode(), invoice2.hashCode());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle zero total amount")
        void shouldHandleZeroTotalAmount() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            
            assertEquals(BigDecimal.ZERO, invoice.getTotalAmount());
            assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
        }

        @Test
        @DisplayName("Should handle very large amounts")
        void shouldHandleVeryLargeAmounts() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Expensive item", new BigDecimal("999999.99"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            assertEquals(new BigDecimal("999999.99"), invoice.getTotalAmount());
        }

        @Test
        @DisplayName("Should handle decimal amounts")
        void shouldHandleDecimalAmounts() {
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 2, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Item with cents", new BigDecimal("123.45"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            assertEquals(new BigDecimal("123.45"), invoice.getTotalAmount());
        }

        @Test
        @DisplayName("Should handle future due dates")
        void shouldHandleFutureDueDates() {
            LocalDate futureDate = LocalDate.now().plusDays(30);
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), futureDate);
            
            assertEquals(futureDate, invoice.getDueDate());
            assertEquals(InvoiceStatus.OPEN, invoice.getStatus());
        }

        @Test
        @DisplayName("Should handle past due dates")
        void shouldHandlePastDueDates() {
            LocalDate pastDate = LocalDate.now().minusDays(30);
            Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), pastDate);
            InvoiceItem item = InvoiceItem.of(invoice, "Dinner", new BigDecimal("100.00"), testCategory, LocalDate.now());
            invoice.addItem(item);
            
            invoice.updateStatus();
            assertEquals(InvoiceStatus.OVERDUE, invoice.getStatus());
        }
    }

    // Helper para setar o id
    private void setInvoiceId(Invoice invoice, Long id) {
        try {
            java.lang.reflect.Field idField = Invoice.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(invoice, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set invoice ID for testing", e);
        }
    }
} 