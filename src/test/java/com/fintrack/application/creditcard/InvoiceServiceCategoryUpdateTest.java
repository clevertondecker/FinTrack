package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceItemJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for InvoiceService category update functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService Category Update Tests")
class InvoiceServiceCategoryUpdateTest {

    @Mock
    private InvoiceItemJpaRepository invoiceItemRepository;

    @Mock
    private CategoryJpaRepository categoryRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private User testUser;
    private CreditCard testCreditCard;
    private Invoice testInvoice;
    private InvoiceItem testItem;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        testUser = User.createLocalUser("Test User", "test@example.com", "password123", roles);
        ReflectionTestUtils.setField(testUser, "id", 1L);

        Bank testBank = Bank.of("001", "Test Bank");
        testCreditCard = CreditCard.of("Test Card", "1234", BigDecimal.valueOf(10000), testUser, testBank);
        ReflectionTestUtils.setField(testCreditCard, "id", 1L);

        testInvoice = Invoice.of(testCreditCard, YearMonth.now(), LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(testInvoice, "id", 1L);

        testItem = InvoiceItem.of(testInvoice, "Amazon Purchase", BigDecimal.valueOf(99.99), null, LocalDate.now());
        ReflectionTestUtils.setField(testItem, "id", 1L);

        testCategory = Category.of("Shopping", "#FF5733");
        ReflectionTestUtils.setField(testCategory, "id", 1L);
    }

    @Nested
    @DisplayName("Update Invoice Item Category")
    class UpdateInvoiceItemCategoryTests {

        @Test
        @DisplayName("Should update item category successfully")
        void shouldUpdateItemCategorySuccessfully() {
            // Given
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.of(testItem));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(invoiceItemRepository.save(testItem)).thenReturn(testItem);

            // When
            InvoiceItem result = invoiceService.updateInvoiceItemCategory(1L, 1L, 1L, testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCategory()).isEqualTo(testCategory);
            verify(invoiceItemRepository).findByIdAndInvoiceCreditCardOwner(1L, testUser);
            verify(categoryRepository).findById(1L);
            verify(invoiceItemRepository).save(testItem);
        }

        @Test
        @DisplayName("Should remove category when categoryId is null")
        void shouldRemoveCategoryWhenCategoryIdIsNull() {
            // Given
            testItem.updateCategory(testCategory); // Set initial category
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.of(testItem));
            when(invoiceItemRepository.save(testItem)).thenReturn(testItem);

            // When
            InvoiceItem result = invoiceService.updateInvoiceItemCategory(1L, 1L, null, testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCategory()).isNull();
            verify(invoiceItemRepository).findByIdAndInvoiceCreditCardOwner(1L, testUser);
            verify(categoryRepository, never()).findById(any());
            verify(invoiceItemRepository).save(testItem);
        }

        @Test
        @DisplayName("Should throw exception when item not found")
        void shouldThrowExceptionWhenItemNotFound() {
            // Given
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> 
                invoiceService.updateInvoiceItemCategory(1L, 1L, 1L, testUser)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessage("Invoice item not found");

            verify(invoiceItemRepository).findByIdAndInvoiceCreditCardOwner(1L, testUser);
            verify(categoryRepository, never()).findById(any());
            verify(invoiceItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            // Given
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.of(testItem));
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> 
                invoiceService.updateInvoiceItemCategory(1L, 1L, 999L, testUser)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessage("Category not found");

            verify(invoiceItemRepository).findByIdAndInvoiceCreditCardOwner(1L, testUser);
            verify(categoryRepository).findById(999L);
            verify(invoiceItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when item belongs to different invoice")
        void shouldThrowExceptionWhenItemBelongsToDifferentInvoice() {
            // Given
            Invoice otherInvoice = Invoice.of(testCreditCard, YearMonth.now(), LocalDate.now().plusDays(30));
            ReflectionTestUtils.setField(otherInvoice, "id", 2L);
            InvoiceItem itemFromOtherInvoice = InvoiceItem.of(otherInvoice, "Other Item",
                BigDecimal.valueOf(50.00), null, LocalDate.now());
            ReflectionTestUtils.setField(itemFromOtherInvoice, "id", 1L);

            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.of(itemFromOtherInvoice));

            // When/Then
            assertThatThrownBy(() -> 
                invoiceService.updateInvoiceItemCategory(1L, 1L, 1L, testUser)
                    // Invoice ID = 1, but item belongs to invoice ID = 2
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessage("Item does not belong to the specified invoice");

            verify(invoiceItemRepository).findByIdAndInvoiceCreditCardOwner(1L, testUser);
            verify(categoryRepository, never()).findById(any());
            verify(invoiceItemRepository, never()).save(any());
        }
    }
}