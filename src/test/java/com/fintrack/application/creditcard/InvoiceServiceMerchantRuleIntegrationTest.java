package com.fintrack.application.creditcard;

import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.InvoiceItemRepository;
import com.fintrack.domain.creditcard.InvoiceRepository;
import com.fintrack.domain.creditcard.MerchantCategoryRule;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.infrastructure.persistence.creditcard.CategoryJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for InvoiceService with MerchantCategorizationService.
 * Tests that manual categorization properly creates and updates merchant rules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceService Merchant Rule Integration Tests")
class InvoiceServiceMerchantRuleIntegrationTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private CreditCardJpaRepository creditCardRepository;

    @Mock
    private CategoryJpaRepository categoryRepository;

    @Mock
    private com.fintrack.domain.user.UserRepository userRepository;

    @Mock
    private com.fintrack.domain.creditcard.InvoiceCalculationService invoiceCalculationService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private MerchantCategorizationService merchantCategorizationService;

    private InvoiceService invoiceService;

    private User testUser;
    private CreditCard testCreditCard;
    private Invoice testInvoice;
    private InvoiceItem testItem;
    private Category foodCategory;
    private Category transportCategory;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
            invoiceRepository, invoiceItemRepository, creditCardRepository,
            categoryRepository, userRepository, invoiceCalculationService, jdbcTemplate,
            merchantCategorizationService);

        testUser = User.createLocalUser("John Doe", "john@example.com", "password123", Set.of(Role.USER));
        ReflectionTestUtils.setField(testUser, "id", 1L);

        Bank testBank = Bank.of("NU", "Nubank");
        testCreditCard = CreditCard.of("Test Card", "1234", new BigDecimal("5000.00"), testUser, testBank);
        ReflectionTestUtils.setField(testCreditCard, "id", 1L);

        testInvoice = Invoice.of(testCreditCard, YearMonth.of(2024, 11), LocalDate.of(2024, 11, 10));
        ReflectionTestUtils.setField(testInvoice, "id", 1L);

        testItem = InvoiceItem.of(
            testInvoice, "UBER *TRIP SAO PAULO", new BigDecimal("50.00"),
            null, LocalDate.of(2024, 10, 15));
        ReflectionTestUtils.setField(testItem, "id", 1L);
        testInvoice.addItem(testItem);

        foodCategory = Category.of("Food", "#FF0000");
        ReflectionTestUtils.setField(foodCategory, "id", 1L);

        transportCategory = Category.of("Transport", "#0000FF");
        ReflectionTestUtils.setField(transportCategory, "id", 2L);
    }

    @Nested
    @DisplayName("updateInvoiceItemCategory with Merchant Rules")
    class UpdateInvoiceItemCategoryWithMerchantRulesTests {

        @Test
        @DisplayName("Should call recordManualCategorization when category is set")
        void shouldCallRecordManualCategorizationWhenCategoryIsSet() {
            // Given
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.of(testItem));
            when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(transportCategory));
            when(invoiceItemRepository.save(any(InvoiceItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            invoiceService.updateInvoiceItemCategory(1L, 1L, 2L, testUser);

            // Then
            verify(merchantCategorizationService).recordManualCategorization(
                eq(testItem), eq(transportCategory), eq(testUser));
        }

        @Test
        @DisplayName("Should NOT call recordManualCategorization when categoryId is null")
        void shouldNotCallRecordManualCategorizationWhenCategoryIsNull() {
            // Given
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.of(testItem));
            when(invoiceItemRepository.save(any(InvoiceItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When - pass null categoryId
            invoiceService.updateInvoiceItemCategory(1L, 1L, null, testUser);

            // Then
            verify(merchantCategorizationService, never()).recordManualCategorization(any(), any(), any());
        }

        @Test
        @DisplayName("Should pass correct item, category and user to recordManualCategorization")
        void shouldPassCorrectParametersToRecordManualCategorization() {
            // Given
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.of(testItem));
            when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(foodCategory));
            when(invoiceItemRepository.save(any(InvoiceItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            invoiceService.updateInvoiceItemCategory(1L, 1L, 1L, testUser);

            // Then
            ArgumentCaptor<InvoiceItem> itemCaptor = ArgumentCaptor.forClass(InvoiceItem.class);
            ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            verify(merchantCategorizationService).recordManualCategorization(
                itemCaptor.capture(), categoryCaptor.capture(), userCaptor.capture());

            assertThat(itemCaptor.getValue()).isEqualTo(testItem);
            assertThat(categoryCaptor.getValue()).isEqualTo(foodCategory);
            assertThat(userCaptor.getValue()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should update item category before calling recordManualCategorization")
        void shouldUpdateItemCategoryBeforeCallingRecordManualCategorization() {
            // Given
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.of(testItem));
            when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(transportCategory));
            when(invoiceItemRepository.save(any(InvoiceItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            InvoiceItem result = invoiceService.updateInvoiceItemCategory(1L, 1L, 2L, testUser);

            // Then
            assertThat(result.getCategory()).isEqualTo(transportCategory);
        }
    }

    @Nested
    @DisplayName("Merchant Rule Creation Flow")
    class MerchantRuleCreationFlowTests {

        @Test
        @DisplayName("Should create new rule on first manual categorization")
        void shouldCreateNewRuleOnFirstManualCategorization() {
            // Given
            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(1L, testUser))
                .thenReturn(Optional.of(testItem));
            when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(transportCategory));
            when(invoiceItemRepository.save(any(InvoiceItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            MerchantCategoryRule newRule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP SAO PAULO", transportCategory);
            when(merchantCategorizationService.recordManualCategorization(any(), any(), any()))
                .thenReturn(newRule);

            // When
            invoiceService.updateInvoiceItemCategory(1L, 1L, 2L, testUser);

            // Then
            verify(merchantCategorizationService).recordManualCategorization(
                testItem, transportCategory, testUser);
        }

        @Test
        @DisplayName("Should work with items that have special characters in description")
        void shouldWorkWithSpecialCharactersInDescription() {
            // Given
            InvoiceItem specialItem = InvoiceItem.of(
                testInvoice, "PAG*RESTAURANTE DO JOÃO", new BigDecimal("75.00"),
                null, LocalDate.of(2024, 10, 16));
            ReflectionTestUtils.setField(specialItem, "id", 2L);

            when(invoiceItemRepository.findByIdAndInvoiceCreditCardOwner(2L, testUser))
                .thenReturn(Optional.of(specialItem));
            when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(foodCategory));
            when(invoiceItemRepository.save(any(InvoiceItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            invoiceService.updateInvoiceItemCategory(1L, 2L, 1L, testUser);

            // Then
            verify(merchantCategorizationService).recordManualCategorization(
                eq(specialItem), eq(foodCategory), eq(testUser));
        }
    }
}
