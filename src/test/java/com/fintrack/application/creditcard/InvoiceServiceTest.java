package com.fintrack.application.creditcard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceCalculationService;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.InvoiceStatus;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.domain.user.UserRepository;
import com.fintrack.dto.creditcard.CreateInvoiceRequest;
import com.fintrack.dto.creditcard.CreateInvoiceItemRequest;
import com.fintrack.dto.creditcard.InvoiceResponse;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceItemJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService Tests")
class InvoiceServiceTest {

    @Mock
    private InvoiceJpaRepository invoiceRepository;

    @Mock
    private InvoiceItemJpaRepository invoiceItemRepository;

    @Mock
    private CreditCardJpaRepository creditCardRepository;

    @Mock
    private CategoryJpaRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvoiceCalculationService invoiceCalculationService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private InvoiceService invoiceService;

    private User testUser;
    private Bank testBank;
    private CreditCard testCreditCard;
    private Invoice testInvoice;
    private Category testCategory;
    private InvoiceItem testInvoiceItem;

    @BeforeEach
    void setUp() throws Exception {
        invoiceService = new InvoiceService(
            invoiceRepository, invoiceItemRepository, creditCardRepository,
            categoryRepository, userRepository, invoiceCalculationService, jdbcTemplate);

        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        testBank = Bank.of("NU", "Nubank");
        testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
        testInvoice = Invoice.of(testCreditCard, YearMonth.of(2024, 2), LocalDate.of(2024, 2, 10));
        testCategory = Category.of("Food", "#FF0000");
        testInvoiceItem = InvoiceItem.of(testInvoice, "Test Item", new BigDecimal("100.00"),
            testCategory, LocalDate.of(2024, 1, 15));
        
        // Add the item to the invoice
        testInvoice.addItem(testInvoiceItem);

        // Set IDs via reflection
        java.lang.reflect.Field invoiceIdField = Invoice.class.getDeclaredField("id");
        invoiceIdField.setAccessible(true);
        invoiceIdField.set(testInvoice, 1L);

        java.lang.reflect.Field itemIdField = InvoiceItem.class.getDeclaredField("id");
        itemIdField.setAccessible(true);
        itemIdField.set(testInvoiceItem, 1L);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create InvoiceService with valid dependencies")
        void shouldCreateInvoiceServiceWithValidDependencies() {
            assertNotNull(invoiceService);
        }
    }

    @Nested
    @DisplayName("findUserByUsername Tests")
    class FindUserByUsernameTests {

        @Test
        @DisplayName("Should find user by valid username")
        void shouldFindUserByValidUsername() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));

            Optional<User> result = invoiceService.findUserByUsername("john@example.com");

            assertTrue(result.isPresent());
            assertEquals(testUser, result.get());
        }

        @Test
        @DisplayName("Should return empty when username is null")
        void shouldReturnEmptyWhenUsernameIsNull() {
            Optional<User> result = invoiceService.findUserByUsername(null);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty when username is empty")
        void shouldReturnEmptyWhenUsernameIsEmpty() {
            Optional<User> result = invoiceService.findUserByUsername("");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should return empty when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

            Optional<User> result = invoiceService.findUserByUsername("nonexistent@example.com");

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("createInvoice Tests")
    class CreateInvoiceTests {

        @Test
        @DisplayName("Should create invoice successfully")
        void shouldCreateInvoiceSuccessfully() {
            CreateInvoiceRequest request =
              new CreateInvoiceRequest(1L, LocalDate.of(2024, 2, 10));

            when(creditCardRepository.findByIdAndOwner(1L, testUser))
              .thenReturn(Optional.of(testCreditCard));

            when(invoiceRepository.save(any())).thenReturn(testInvoice);

            Invoice result = invoiceService.createInvoice(request, testUser);

            assertNotNull(result);
            assertEquals(testInvoice, result);
        }

        @Test
        @DisplayName("Should throw exception when credit card not found")
        void shouldThrowExceptionWhenCreditCardNotFound() {
            CreateInvoiceRequest request =
              new CreateInvoiceRequest(999L, LocalDate.of(2024, 2, 10));

            when(creditCardRepository.findByIdAndOwner(999L, testUser))
              .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                invoiceService.createInvoice(request, testUser));
        }

        @Test
        @DisplayName("Should throw exception when due date is null")
        void shouldThrowExceptionWhenDueDateIsNull() {
            CreateInvoiceRequest request = new CreateInvoiceRequest(1L, null);
            when(creditCardRepository.findByIdAndOwner(1L, testUser))
                .thenReturn(Optional.of(testCreditCard));
            assertThrows(NullPointerException.class, () ->
                invoiceService.createInvoice(request, testUser));
        }
    }

    @Nested
    @DisplayName("getUserInvoices Tests")
    class GetUserInvoicesTests {

        @Test
        @DisplayName("Should return user invoices")
        void shouldReturnUserInvoices() {
            List<Invoice> expectedInvoices = List.of(testInvoice);
            when(invoiceRepository.findByCreditCardOwner(testUser))
              .thenReturn(expectedInvoices);

            List<Invoice> result = invoiceService.getUserInvoices(testUser);

            assertEquals(expectedInvoices, result);
        }
    }

    @Nested
    @DisplayName("getInvoicesByCreditCard Tests")
    class GetInvoicesByCreditCardTests {

        @Test
        @DisplayName("Should return invoices for credit card")
        void shouldReturnInvoicesForCreditCard() {
            List<Invoice> expectedInvoices = List.of(testInvoice);
            when(creditCardRepository.findByIdAndOwner(1L, testUser))
              .thenReturn(Optional.of(testCreditCard));

            when(invoiceRepository.findByCreditCard(testCreditCard))
              .thenReturn(expectedInvoices);

            List<Invoice> result = invoiceService.getInvoicesByCreditCard(1L, testUser);

            assertEquals(expectedInvoices, result);
        }

        @Test
        @DisplayName("Should throw exception when credit card not found")
        void shouldThrowExceptionWhenCreditCardNotFound() {
            when(creditCardRepository.findByIdAndOwner(999L, testUser)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                invoiceService.getInvoicesByCreditCard(999L, testUser));
        }
    }

    @Nested
    @DisplayName("getInvoice Tests")
    class GetInvoiceTests {

        @Test
        @DisplayName("Should return invoice by ID")
        void shouldReturnInvoiceById() {
            when(invoiceRepository.findByIdAndCreditCardOwner(1L, testUser))
              .thenReturn(Optional.of(testInvoice));

            Invoice result = invoiceService.getInvoice(1L, testUser);

            assertEquals(testInvoice, result);
        }

        @Test
        @DisplayName("Should throw exception when invoice not found")
        void shouldThrowExceptionWhenInvoiceNotFound() {
            when(invoiceRepository.findByIdAndCreditCardOwner(999L, testUser))
              .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                invoiceService.getInvoice(999L, testUser));
        }
    }

    @Nested
    @DisplayName("createInvoiceItem Tests")
    class CreateInvoiceItemTests {

        @Test
        @DisplayName("Should create invoice item successfully")
        void shouldCreateInvoiceItemSuccessfully() {
            CreateInvoiceItemRequest request = new CreateInvoiceItemRequest(
                "Test Item", new BigDecimal("100.00"), 1L, LocalDate.of(2024, 1, 15));

            when(invoiceRepository.findByIdAndCreditCardOwner(1L, testUser))
              .thenReturn(Optional.of(testInvoice));

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(invoiceRepository.save(any())).thenReturn(testInvoice);

            Invoice result = invoiceService.createInvoiceItem(1L, request, testUser);

            assertNotNull(result);
            assertEquals(testInvoice, result);
        }

        @Test
        @DisplayName("Should create invoice item without category")
        void shouldCreateInvoiceItemWithoutCategory() {
            CreateInvoiceItemRequest request = new CreateInvoiceItemRequest(
                "Test Item", new BigDecimal("100.00"), null, LocalDate.of(2024, 1, 15));

            when(invoiceRepository.findByIdAndCreditCardOwner(1L, testUser))
              .thenReturn(Optional.of(testInvoice));
            when(invoiceRepository.save(any())).thenReturn(testInvoice);

            Invoice result = invoiceService.createInvoiceItem(1L, request, testUser);

            assertNotNull(result);
            assertEquals(testInvoice, result);
        }

        @Test
        @DisplayName("Should throw exception when invoice not found")
        void shouldThrowExceptionWhenInvoiceNotFound() {
            CreateInvoiceItemRequest request = new CreateInvoiceItemRequest(
                "Test Item", new BigDecimal("100.00"), 1L, LocalDate.of(2024, 1, 15));

            when(invoiceRepository.findByIdAndCreditCardOwner(999L, testUser))
              .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                invoiceService.createInvoiceItem(999L, request, testUser));
        }
    }

    @Nested
    @DisplayName("getInvoiceItems Tests")
    class GetInvoiceItemsTests {

        @Test
        @DisplayName("Should return invoice items")
        void shouldReturnInvoiceItems() {
            List<InvoiceItem> expectedItems = List.of(testInvoiceItem);

            when(invoiceRepository.findByIdAndCreditCardOwner(1L, testUser))
              .thenReturn(Optional.of(testInvoice));
            when(invoiceItemRepository.findByInvoice(testInvoice)).thenReturn(expectedItems);

            List<InvoiceItem> result = invoiceService.getInvoiceItems(1L, testUser);

            assertEquals(expectedItems, result);
        }

        @Test
        @DisplayName("Should throw exception when invoice not found")
        void shouldThrowExceptionWhenInvoiceNotFound() {
            when(invoiceRepository.findByIdAndCreditCardOwner(999L, testUser))
              .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                invoiceService.getInvoiceItems(999L, testUser));
        }
    }

    @Nested
    @DisplayName("getInvoiceItem Tests")
    class GetInvoiceItemTests {

        @Test
        @DisplayName("Should return invoice item by ID")
        void shouldReturnInvoiceItemById() {
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
              .thenReturn(Optional.of(testInvoiceItem));

            InvoiceItem result = invoiceService.getInvoiceItem(1L, 1L, testUser);

            assertEquals(testInvoiceItem, result);
        }

        @Test
        @DisplayName("Should throw exception when item not found")
        void shouldThrowExceptionWhenItemNotFound() {
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(999L, testUser))
              .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                invoiceService.getInvoiceItem(1L, 999L, testUser));
        }

        @Test
        @DisplayName("Should throw exception when item belongs to different invoice")
        void shouldThrowExceptionWhenItemBelongsToDifferentInvoice() throws Exception {
            Invoice otherInvoice =
              Invoice.of(testCreditCard, YearMonth.of(2024, 3), LocalDate.of(2024, 3, 10));

            InvoiceItem otherItem = InvoiceItem.of(otherInvoice, "Other Item",
              new BigDecimal("50.00"), testCategory, LocalDate.of(2024, 2, 15));

            // Set IDs via reflection
            java.lang.reflect.Field invoiceIdField = Invoice.class.getDeclaredField("id");
            invoiceIdField.setAccessible(true);
            invoiceIdField.set(otherInvoice, 99L);
            java.lang.reflect.Field itemIdField = InvoiceItem.class.getDeclaredField("id");
            itemIdField.setAccessible(true);
            itemIdField.set(otherItem, 2L);

            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
              .thenReturn(Optional.of(otherItem));

            assertThrows(IllegalArgumentException.class, () ->
                invoiceService.getInvoiceItem(999L, 1L, testUser));
        }
    }

    @Nested
    @DisplayName("deleteInvoiceItem Tests")
    class DeleteInvoiceItemTests {

        @Test
        @DisplayName("Should delete invoice item successfully")
        void shouldDeleteInvoiceItemSuccessfully() {
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
              .thenReturn(Optional.of(testInvoiceItem));
            when(invoiceRepository.save(any())).thenReturn(testInvoice);

            Invoice result = invoiceService.deleteInvoiceItem(1L, 1L, testUser);

            assertNotNull(result);
            assertEquals(testInvoice, result);
        }

        @Test
        @DisplayName("Should throw exception when item not found")
        void shouldThrowExceptionWhenItemNotFound() {
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(999L, testUser))
              .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                invoiceService.deleteInvoiceItem(1L, 999L, testUser));
        }

        @Test
        @DisplayName("Should throw exception when item belongs to different invoice")
        void shouldThrowExceptionWhenItemBelongsToDifferentInvoice() throws Exception {
            Invoice otherInvoice = Invoice.of(testCreditCard, YearMonth.of(2024, 3),
              LocalDate.of(2024, 3, 10));

            InvoiceItem otherItem = InvoiceItem.of(otherInvoice, "Other Item",
              new BigDecimal("50.00"), testCategory, LocalDate.of(2024, 2, 15));

            // Set IDs via reflection
            java.lang.reflect.Field invoiceIdField = Invoice.class.getDeclaredField("id");
            invoiceIdField.setAccessible(true);
            invoiceIdField.set(otherInvoice, 99L);
            java.lang.reflect.Field itemIdField = InvoiceItem.class.getDeclaredField("id");
            itemIdField.setAccessible(true);
            itemIdField.set(otherItem, 2L);

            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
              .thenReturn(Optional.of(otherItem));

            assertThrows(IllegalArgumentException.class, () ->
                invoiceService.deleteInvoiceItem(999L, 1L, testUser));
        }
    }

    @Nested
    @DisplayName("DTO Conversion Tests")
    class DtoConversionTests {

        @Test
        @DisplayName("Should convert invoice to DTO")
        void shouldConvertInvoiceToDto() {
            InvoiceResponse result = invoiceService.toInvoiceResponse(testInvoice);

            assertNotNull(result);
            assertEquals(testInvoice.getId(), result.id());
            assertEquals(testInvoice.getCreditCard().getId(), result.creditCardId());
            assertEquals(testInvoice.getCreditCard().getName(), result.creditCardName());
            assertEquals(testInvoice.getDueDate(), result.dueDate());
            assertEquals(testInvoice.getTotalAmount(), result.totalAmount());
            assertEquals(testInvoice.getPaidAmount(), result.paidAmount());
            assertEquals(testInvoice.getStatus().name(), result.status());
        }

        @Test
        @DisplayName("Should convert invoice item to DTO")
        void shouldConvertInvoiceItemToDto() {
            Map<String, Object> result = invoiceService.toInvoiceItemDto(testInvoiceItem);

            assertNotNull(result);
            assertEquals(testInvoiceItem.getId(), result.get("id"));
            assertEquals(testInvoiceItem.getInvoice().getId(), result.get("invoiceId"));
            assertEquals(testInvoiceItem.getDescription(), result.get("description"));
            assertEquals(testInvoiceItem.getAmount(), result.get("amount"));
            assertEquals(testInvoiceItem.getCategory().getName(), result.get("category"));
            assertEquals(testInvoiceItem.getPurchaseDate().toString(), result.get("purchaseDate"));
        }

        @Test
        @DisplayName("Should convert invoice item to DTO without category")
        void shouldConvertInvoiceItemToDtoWithoutCategory() {
            InvoiceItem itemWithoutCategory = InvoiceItem.of(testInvoice, "Test Item",
              new BigDecimal("100.00"), null, LocalDate.of(2024, 1, 15));

            Map<String, Object> result = invoiceService.toInvoiceItemDto(itemWithoutCategory);

            assertNotNull(result);
            assertNull(result.get("category"));
        }

        @Test
        @DisplayName("Should convert list of invoices to DTOs")
        void shouldConvertListOfInvoicesToDtos() {
            List<Invoice> invoices = List.of(testInvoice);
            List<InvoiceResponse> result = invoiceService.toInvoiceResponseList(invoices);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testInvoice.getId(), result.get(0).id());
        }

        @Test
        @DisplayName("Should convert list of invoice items to DTOs")
        void shouldConvertListOfInvoiceItemsToDtos() {
            List<InvoiceItem> items = List.of(testInvoiceItem);
            List<Map<String, Object>> result = invoiceService.toInvoiceItemDtos(items);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Test
    @DisplayName("Should allow adding items to closed invoice through service")
    void shouldAllowAddingItemsToClosedInvoiceThroughService() {
        // Given: A closed invoice (no items, past due date)
        Invoice invoice = Invoice.of(testCreditCard, YearMonth.of(2024, 1), LocalDate.of(2024, 1, 10));
        invoice.updateStatus(); // Force status calculation
        
        // Mock repository calls
        when(invoiceRepository.findByIdAndCreditCardOwner(1L, testUser)).thenReturn(Optional.of(invoice));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        assertEquals(InvoiceStatus.CLOSED, invoice.getStatus());
        assertEquals(BigDecimal.ZERO, invoice.getTotalAmount());
        
        // When: Adding an item through the service
        CreateInvoiceItemRequest request = new CreateInvoiceItemRequest(
            "Test Item",
            new BigDecimal("100.00"),
            1L, // categoryId
            LocalDate.now()
        );
        
        Invoice updatedInvoice = invoiceService.createInvoiceItem(1L, request, testUser);
        
        // Then: Invoice should be updated and status should change
        assertEquals(new BigDecimal("100.00"), updatedInvoice.getTotalAmount());
        assertEquals(InvoiceStatus.OVERDUE, updatedInvoice.getStatus());
        assertEquals(1, updatedInvoice.getItems().size());
    }
}