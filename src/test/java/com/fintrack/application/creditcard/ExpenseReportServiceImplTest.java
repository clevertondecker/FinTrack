package com.fintrack.application.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceCalculationService;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.InvoiceRepository;
import com.fintrack.domain.creditcard.ItemShare;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.creditcard.ExpenseDetailResponse;

/**
 * Unit tests for ExpenseReportServiceImpl.
 * Tests the business logic for generating expense reports grouped by category.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseReportService Tests")
class ExpenseReportServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceCalculationService invoiceCalculationService;

    @InjectMocks
    private ExpenseReportServiceImpl expenseReportService;

    private User cardOwner;
    private User otherUser;
    private Bank testBank;
    private CreditCard testCreditCard;
    private YearMonth testMonth;
    private Category foodCategory;
    private Category transportCategory;

    @BeforeEach
    void setUp() throws Exception {
        cardOwner = User.of("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        otherUser = User.of("Jane Smith", "jane@example.com", "password456", Set.of(Role.USER));
        
        // Set user IDs for equals() to work correctly
        java.lang.reflect.Field cardOwnerIdField = User.class.getDeclaredField("id");
        cardOwnerIdField.setAccessible(true);
        cardOwnerIdField.set(cardOwner, 1L);
        
        java.lang.reflect.Field otherUserIdField = User.class.getDeclaredField("id");
        otherUserIdField.setAccessible(true);
        otherUserIdField.set(otherUser, 2L);
        
        testBank = Bank.of("NU", "Nubank");
        testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), cardOwner, testBank);
        testMonth = YearMonth.of(2024, 11);
        foodCategory = Category.of("Food", "#FF0000");
        transportCategory = Category.of("Transport", "#0000FF");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create service with valid dependencies")
        void shouldCreateServiceWithValidDependencies() {
            ExpenseReportServiceImpl service = new ExpenseReportServiceImpl(
                    invoiceRepository, invoiceCalculationService);

            assertThat(service).isNotNull();
        }
    }

    @Nested
    @DisplayName("getExpensesByCategory Tests")
    class GetExpensesByCategoryTests {

        @Test
        @DisplayName("Should return empty map when no invoices exist for month")
        void shouldReturnEmptyMapWhenNoInvoicesExist() {
            // Given
            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of());

            // When
            Map<Category, BigDecimal> result = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(result).isEmpty();
            verify(invoiceRepository).findByMonth(testMonth);
        }

        @Test
        @DisplayName("Should return expenses grouped by category for card owner with unshared items")
        void shouldReturnExpensesGroupedByCategoryForCardOwner() throws Exception {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem foodItem = InvoiceItem.of(invoice, "Groceries", new BigDecimal("100.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            InvoiceItem transportItem = InvoiceItem.of(invoice, "Uber", new BigDecimal("50.00"), 
                    transportCategory, LocalDate.of(2024, 10, 20));
            
            // Set category IDs to ensure proper comparison
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            java.lang.reflect.Field transportIdField = Category.class.getDeclaredField("id");
            transportIdField.setAccessible(true);
            transportIdField.set(transportCategory, 2L);
            
            invoice.addItem(foodItem);
            invoice.addItem(transportItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            Map<Category, BigDecimal> result = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(foodCategory)).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(result.get(transportCategory)).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("Should return expenses for shared items")
        void shouldReturnExpensesForSharedItems() throws Exception {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem sharedItem = InvoiceItem.of(invoice, "Shared Meal", new BigDecimal("200.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            
            // Share 50% with otherUser, 50% unshared (goes to card owner)
            ItemShare otherUserShare = ItemShare.of(otherUser, sharedItem, new BigDecimal("0.50"), 
                    new BigDecimal("100.00"), true);
            sharedItem.addShare(otherUserShare);
            
            // Set category ID for proper comparison
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            invoice.addItem(sharedItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            Map<Category, BigDecimal> otherUserResult = expenseReportService.getExpensesByCategory(otherUser, testMonth);
            Map<Category, BigDecimal> cardOwnerResult = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(otherUserResult.get(foodCategory)).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(cardOwnerResult.get(foodCategory)).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should handle uncategorized items")
        void shouldHandleUncategorizedItems() {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem uncategorizedItem = InvoiceItem.of(invoice, "Misc", new BigDecimal("75.00"), 
                    null, LocalDate.of(2024, 10, 15));
            
            invoice.addItem(uncategorizedItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            Map<Category, BigDecimal> result = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(result).hasSize(1);
            Category uncategorized = result.keySet().stream()
                    .filter(c -> c.getName().equals("Sem categoria"))
                    .findFirst()
                    .orElseThrow();
            assertThat(result.get(uncategorized)).isEqualByComparingTo(new BigDecimal("75.00"));
        }

        @Test
        @DisplayName("Should return zero for user who is not card owner and has no shares")
        void shouldReturnZeroForUserWithoutShares() throws Exception {
            // Given - Item with no shares, user is not card owner
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem unsharedItem = InvoiceItem.of(invoice, "Personal Item", new BigDecimal("100.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            // Item has no shares, so only card owner should get it
            
            // Set category ID (even though result should be empty, we need it for the test setup)
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            invoice.addItem(unsharedItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When - otherUser is not card owner and has no shares
            Map<Category, BigDecimal> result = expenseReportService.getExpensesByCategory(otherUser, testMonth);

            // Then - otherUser should get nothing
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should aggregate multiple items in same category")
        void shouldAggregateMultipleItemsInSameCategory() throws Exception {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem item1 = InvoiceItem.of(invoice, "Item 1", new BigDecimal("50.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            InvoiceItem item2 = InvoiceItem.of(invoice, "Item 2", new BigDecimal("75.00"), 
                    foodCategory, LocalDate.of(2024, 10, 20));
            
            // Set category ID for proper aggregation
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            invoice.addItem(item1);
            invoice.addItem(item2);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            Map<Category, BigDecimal> result = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(foodCategory)).isEqualByComparingTo(new BigDecimal("125.00"));
        }

        @Test
        @DisplayName("Should handle multiple invoices in same month")
        void shouldHandleMultipleInvoicesInSameMonth() throws Exception {
            // Given
            Invoice invoice1 = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem item1 = InvoiceItem.of(invoice1, "Item 1", new BigDecimal("100.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            invoice1.addItem(item1);

            Invoice invoice2 = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 15));
            InvoiceItem item2 = InvoiceItem.of(invoice2, "Item 2", new BigDecimal("200.00"), 
                    foodCategory, LocalDate.of(2024, 10, 20));
            invoice2.addItem(item2);

            // Set category ID for proper aggregation
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice1, invoice2));

            // When
            Map<Category, BigDecimal> result = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(foodCategory)).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("Should filter by invoice month, not due date")
        void shouldFilterByInvoiceMonthNotDueDate() throws Exception {
            // Given - Invoice with month 11 but due date in December
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 12, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Item", new BigDecimal("100.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            
            // Set category ID
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            invoice.addItem(item);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            Map<Category, BigDecimal> result = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(foodCategory)).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("getTotalExpenses Tests")
    class GetTotalExpensesTests {

        @Test
        @DisplayName("Should return zero when no invoices exist")
        void shouldReturnZeroWhenNoInvoicesExist() {
            // Given
            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of());

            // When
            BigDecimal result = expenseReportService.getTotalExpenses(cardOwner, testMonth);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return total using InvoiceCalculationService")
        void shouldReturnTotalUsingInvoiceCalculationService() throws Exception {
            // Given
            Invoice invoice1 = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            Invoice invoice2 = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 15));

            // Set invoice IDs for equals() to work
            java.lang.reflect.Field invoice1IdField = Invoice.class.getDeclaredField("id");
            invoice1IdField.setAccessible(true);
            invoice1IdField.set(invoice1, 1L);
            
            java.lang.reflect.Field invoice2IdField = Invoice.class.getDeclaredField("id");
            invoice2IdField.setAccessible(true);
            invoice2IdField.set(invoice2, 2L);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice1, invoice2));
            when(invoiceCalculationService.calculateUserShare(any(Invoice.class), eq(cardOwner)))
                    .thenAnswer(invocation -> {
                        Invoice inv = invocation.getArgument(0);
                        if (inv.equals(invoice1)) {
                            return new BigDecimal("150.00");
                        } else if (inv.equals(invoice2)) {
                            return new BigDecimal("250.00");
                        }
                        return BigDecimal.ZERO;
                    });

            // When
            BigDecimal result = expenseReportService.getTotalExpenses(cardOwner, testMonth);

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("400.00"));
            verify(invoiceCalculationService, times(2)).calculateUserShare(any(Invoice.class), eq(cardOwner));
        }
    }

    @Nested
    @DisplayName("getTotalByCategory Tests")
    class GetTotalByCategoryTests {

        @Test
        @DisplayName("Should return zero when category has no expenses")
        void shouldReturnZeroWhenCategoryHasNoExpenses() {
            // Given
            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of());

            // When
            BigDecimal result = expenseReportService.getTotalByCategory(cardOwner, testMonth, foodCategory);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return total for specific category")
        void shouldReturnTotalForSpecificCategory() throws Exception {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem foodItem = InvoiceItem.of(invoice, "Food", new BigDecimal("100.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            InvoiceItem transportItem = InvoiceItem.of(invoice, "Transport", new BigDecimal("50.00"), 
                    transportCategory, LocalDate.of(2024, 10, 20));
            
            // Set category IDs to ensure proper comparison
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            java.lang.reflect.Field transportIdField = Category.class.getDeclaredField("id");
            transportIdField.setAccessible(true);
            transportIdField.set(transportCategory, 2L);
            
            invoice.addItem(foodItem);
            invoice.addItem(transportItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            BigDecimal result = expenseReportService.getTotalByCategory(cardOwner, testMonth, foodCategory);

            // Then
            assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    @Nested
    @DisplayName("getExpenseDetails Tests")
    class GetExpenseDetailsTests {

        @Test
        @DisplayName("Should return empty list when no expenses match category")
        void shouldReturnEmptyListWhenNoExpensesMatchCategory() {
            // Given
            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of());

            // When
            List<ExpenseDetailResponse> result = expenseReportService.getExpenseDetails(
                    cardOwner, testMonth, foodCategory);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return details for shared items with share ID")
        void shouldReturnDetailsForSharedItemsWithShareId() throws Exception {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem sharedItem = InvoiceItem.of(invoice, "Shared Meal", new BigDecimal("200.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            
            ItemShare share = ItemShare.of(otherUser, sharedItem, new BigDecimal("0.50"), 
                    new BigDecimal("100.00"), true);
            sharedItem.addShare(share);
            
            // Set share ID via reflection
            java.lang.reflect.Field shareIdField = ItemShare.class.getDeclaredField("id");
            shareIdField.setAccessible(true);
            shareIdField.set(share, 1L);
            
            // Set category ID
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            invoice.addItem(sharedItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            List<ExpenseDetailResponse> result = expenseReportService.getExpenseDetails(
                    otherUser, testMonth, foodCategory);

            // Then
            assertThat(result).hasSize(1);
            ExpenseDetailResponse detail = result.get(0);
            assertThat(detail.shareId()).isEqualTo(1L);
            assertThat(detail.itemDescription()).isEqualTo("Shared Meal");
            assertThat(detail.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should return details for unshared items with null share ID")
        void shouldReturnDetailsForUnsharedItemsWithNullShareId() throws Exception {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem unsharedItem = InvoiceItem.of(invoice, "Personal Item", new BigDecimal("100.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            
            // Set category ID
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            invoice.addItem(unsharedItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            List<ExpenseDetailResponse> result = expenseReportService.getExpenseDetails(
                    cardOwner, testMonth, foodCategory);

            // Then
            assertThat(result).hasSize(1);
            ExpenseDetailResponse detail = result.get(0);
            assertThat(detail.shareId()).isNull();
            assertThat(detail.itemDescription()).isEqualTo("Personal Item");
            assertThat(detail.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should return details for uncategorized items")
        void shouldReturnDetailsForUncategorizedItems() {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem uncategorizedItem = InvoiceItem.of(invoice, "Misc", new BigDecimal("75.00"), 
                    null, LocalDate.of(2024, 10, 15));
            
            invoice.addItem(uncategorizedItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            List<ExpenseDetailResponse> result = expenseReportService.getExpenseDetails(
                    cardOwner, testMonth, null);

            // Then
            assertThat(result).hasSize(1);
            ExpenseDetailResponse detail = result.get(0);
            assertThat(detail.itemDescription()).isEqualTo("Misc");
            assertThat(detail.amount()).isEqualByComparingTo(new BigDecimal("75.00"));
        }

        @Test
        @DisplayName("Should filter details by category")
        void shouldFilterDetailsByCategory() throws Exception {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem foodItem = InvoiceItem.of(invoice, "Food", new BigDecimal("100.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            InvoiceItem transportItem = InvoiceItem.of(invoice, "Transport", new BigDecimal("50.00"), 
                    transportCategory, LocalDate.of(2024, 10, 20));
            
            // Set category IDs
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            java.lang.reflect.Field transportIdField = Category.class.getDeclaredField("id");
            transportIdField.setAccessible(true);
            transportIdField.set(transportCategory, 2L);
            
            invoice.addItem(foodItem);
            invoice.addItem(transportItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            List<ExpenseDetailResponse> foodResult = expenseReportService.getExpenseDetails(
                    cardOwner, testMonth, foodCategory);
            List<ExpenseDetailResponse> transportResult = expenseReportService.getExpenseDetails(
                    cardOwner, testMonth, transportCategory);

            // Then
            assertThat(foodResult).hasSize(1);
            assertThat(foodResult.get(0).itemDescription()).isEqualTo("Food");
            assertThat(transportResult).hasSize(1);
            assertThat(transportResult.get(0).itemDescription()).isEqualTo("Transport");
        }

        @Test
        @DisplayName("Should not return details for user without shares")
        void shouldNotReturnDetailsForUserWithoutShares() throws Exception {
            // Given - Item with no shares, user is not card owner
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem unsharedItem = InvoiceItem.of(invoice, "Personal Item", new BigDecimal("100.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            // Item has no shares, so only card owner should get it
            
            // Set category ID (even though result should be empty, we need it for the test setup)
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            invoice.addItem(unsharedItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When - otherUser is not card owner and has no shares
            List<ExpenseDetailResponse> result = expenseReportService.getExpenseDetails(
                    otherUser, testMonth, foodCategory);

            // Then - otherUser should get no details
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle items with zero amount")
        void shouldHandleItemsWithZeroAmount() {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem zeroItem = InvoiceItem.of(invoice, "Zero Item", BigDecimal.ZERO, 
                    foodCategory, LocalDate.of(2024, 10, 15));
            
            invoice.addItem(zeroItem);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            Map<Category, BigDecimal> result = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle invoice with no items")
        void shouldHandleInvoiceWithNoItems() {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            Map<Category, BigDecimal> result = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle partial shares correctly")
        void shouldHandlePartialSharesCorrectly() throws Exception {
            // Given
            Invoice invoice = Invoice.of(testCreditCard, testMonth, LocalDate.of(2024, 11, 10));
            InvoiceItem item = InvoiceItem.of(invoice, "Item", new BigDecimal("100.00"), 
                    foodCategory, LocalDate.of(2024, 10, 15));
            
            // Share 30% with otherUser, 70% unshared (goes to card owner)
            ItemShare share = ItemShare.of(otherUser, item, new BigDecimal("0.30"), 
                    new BigDecimal("30.00"), true);
            item.addShare(share);
            
            // Set category ID
            java.lang.reflect.Field foodIdField = Category.class.getDeclaredField("id");
            foodIdField.setAccessible(true);
            foodIdField.set(foodCategory, 1L);
            
            invoice.addItem(item);

            when(invoiceRepository.findByMonth(testMonth)).thenReturn(List.of(invoice));

            // When
            Map<Category, BigDecimal> otherUserResult = expenseReportService.getExpensesByCategory(otherUser, testMonth);
            Map<Category, BigDecimal> cardOwnerResult = expenseReportService.getExpensesByCategory(cardOwner, testMonth);

            // Then
            assertThat(otherUserResult.get(foodCategory)).isEqualByComparingTo(new BigDecimal("30.00"));
            // Card owner gets the unshared amount (100 - 30 = 70)
            assertThat(cardOwnerResult.get(foodCategory)).isEqualByComparingTo(new BigDecimal("70.00"));
        }
    }
}

