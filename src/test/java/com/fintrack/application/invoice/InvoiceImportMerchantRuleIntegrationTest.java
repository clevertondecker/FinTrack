package com.fintrack.application.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.application.creditcard.MerchantCategorizationService;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.Category;
import com.fintrack.domain.creditcard.CategorizationSource;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.creditcard.MerchantCategoryRule;
import com.fintrack.domain.invoice.ImportSource;
import com.fintrack.domain.invoice.InvoiceImport;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.infrastructure.parsing.PdfInvoiceParser;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceJpaRepository;
import com.fintrack.infrastructure.persistence.invoice.InvoiceImportJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for InvoiceImportService with MerchantCategorizationService.
 * Tests that imported items are properly auto-categorized using merchant rules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceImportService Merchant Rule Integration Tests")
class InvoiceImportMerchantRuleIntegrationTest {

    @Mock
    private InvoiceImportJpaRepository invoiceImportRepository;

    @Mock
    private CreditCardJpaRepository creditCardRepository;

    @Mock
    private InvoiceJpaRepository invoiceRepository;

    @Mock
    private PdfInvoiceParser pdfInvoiceParser;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MerchantCategorizationService merchantCategorizationService;

    @InjectMocks
    private InvoiceImportService invoiceImportService;

    private User testUser;
    private CreditCard testCreditCard;
    private Bank testBank;
    private Invoice testInvoice;
    private InvoiceImport testImport;
    private Category transportCategory;

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        testUser = User.createLocalUser("Test User", "test@example.com", "password123", roles);
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testBank = Bank.of("001", "Test Bank");
        testCreditCard = CreditCard.of("Test Card", "1234", BigDecimal.valueOf(10000), testUser, testBank);
        ReflectionTestUtils.setField(testCreditCard, "id", 1L);

        testInvoice = Invoice.of(testCreditCard, YearMonth.of(2024, 11), LocalDate.of(2024, 11, 10));
        ReflectionTestUtils.setField(testInvoice, "id", 1L);

        testImport = InvoiceImport.of(testUser, ImportSource.PDF, "test.pdf", "/tmp/test.pdf");
        testImport.setCreditCard(testCreditCard);
        ReflectionTestUtils.setField(testImport, "id", 1L);

        transportCategory = Category.of("Transport", "#0000FF");
        ReflectionTestUtils.setField(transportCategory, "id", 1L);

        // Set upload directory for tests
        ReflectionTestUtils.setField(invoiceImportService, "uploadDirectory", System.getProperty("java.io.tmpdir"));
    }

    @Nested
    @DisplayName("processImportAsync with Merchant Rules")
    class ProcessImportAsyncWithMerchantRulesTests {

        @Test
        @DisplayName("Should call applyRulesToItems after creating invoice items")
        void shouldCallApplyRulesToItemsAfterCreatingInvoiceItems() throws IOException {
            // Given
            ParsedInvoiceData parsedData = new ParsedInvoiceData(
                "Test Card",     // creditCardName
                "1234",          // cardNumber
                LocalDate.of(2024, 11, 10), // dueDate
                new BigDecimal("89.90"),    // totalAmount
                List.of(
                    new ParsedInvoiceData.ParsedInvoiceItem(
                        "UBER *TRIP SAO PAULO",
                        new BigDecimal("50.00"),
                        LocalDate.of(2024, 10, 15),
                        null, 1, 1, 0.9
                    ),
                    new ParsedInvoiceData.ParsedInvoiceItem(
                        "NETFLIX.COM",
                        new BigDecimal("39.90"),
                        LocalDate.of(2024, 10, 16),
                        null, 1, 1, 0.9
                    )
                ),
                "Test Bank",     // bankName
                YearMonth.of(2024, 11), // invoiceMonth
                0.95             // confidence
            );

            when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(testImport));
            when(invoiceImportRepository.save(any(InvoiceImport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(pdfInvoiceParser.parsePdf(any())).thenReturn(parsedData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(invoiceRepository.findByCreditCardAndMonth(any(), any()))
                .thenReturn(List.of(testInvoice));
            when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(invoiceImportRepository.findByCreatedInvoiceId(any())).thenReturn(List.of());
            when(merchantCategorizationService.applyRulesToItems(anyList(), any(User.class)))
                .thenReturn(1);

            // When
            invoiceImportService.processImportAsync(1L);

            // Then
            verify(merchantCategorizationService).applyRulesToItems(anyList(), eq(testUser));
        }

        @Test
        @DisplayName("Should pass credit card owner to applyRulesToItems")
        void shouldPassCreditCardOwnerToApplyRulesToItems() throws IOException {
            // Given
            ParsedInvoiceData parsedData = new ParsedInvoiceData(
                "Test Card",     // creditCardName
                "1234",          // cardNumber
                LocalDate.of(2024, 11, 10), // dueDate
                new BigDecimal("50.00"),    // totalAmount
                List.of(
                    new ParsedInvoiceData.ParsedInvoiceItem(
                        "UBER *TRIP",
                        new BigDecimal("50.00"),
                        LocalDate.of(2024, 10, 15),
                        null, 1, 1, 0.9
                    )
                ),
                "Test Bank",     // bankName
                YearMonth.of(2024, 11), // invoiceMonth
                0.95             // confidence
            );

            when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(testImport));
            when(invoiceImportRepository.save(any(InvoiceImport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(pdfInvoiceParser.parsePdf(any())).thenReturn(parsedData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(invoiceRepository.findByCreditCardAndMonth(any(), any()))
                .thenReturn(List.of(testInvoice));
            when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(invoiceImportRepository.findByCreatedInvoiceId(any())).thenReturn(List.of());
            when(merchantCategorizationService.applyRulesToItems(anyList(), any(User.class)))
                .thenReturn(0);

            // When
            invoiceImportService.processImportAsync(1L);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(merchantCategorizationService).applyRulesToItems(anyList(), userCaptor.capture());

            assertThat(userCaptor.getValue()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should pass all invoice items to applyRulesToItems")
        void shouldPassAllInvoiceItemsToApplyRulesToItems() throws IOException {
            // Given
            ParsedInvoiceData parsedData = new ParsedInvoiceData(
                "Test Card",     // creditCardName
                "1234",          // cardNumber
                LocalDate.of(2024, 11, 10), // dueDate
                new BigDecimal("60.00"),    // totalAmount
                List.of(
                    new ParsedInvoiceData.ParsedInvoiceItem(
                        "ITEM 1",
                        new BigDecimal("10.00"),
                        LocalDate.of(2024, 10, 15),
                        null, 1, 1, 0.9
                    ),
                    new ParsedInvoiceData.ParsedInvoiceItem(
                        "ITEM 2",
                        new BigDecimal("20.00"),
                        LocalDate.of(2024, 10, 16),
                        null, 1, 1, 0.9
                    ),
                    new ParsedInvoiceData.ParsedInvoiceItem(
                        "ITEM 3",
                        new BigDecimal("30.00"),
                        LocalDate.of(2024, 10, 17),
                        null, 1, 1, 0.9
                    )
                ),
                "Test Bank",     // bankName
                YearMonth.of(2024, 11), // invoiceMonth
                0.95             // confidence
            );

            when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(testImport));
            when(invoiceImportRepository.save(any(InvoiceImport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(pdfInvoiceParser.parsePdf(any())).thenReturn(parsedData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(invoiceRepository.findByCreditCardAndMonth(any(), any()))
                .thenReturn(List.of(testInvoice));
            when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(invoiceImportRepository.findByCreatedInvoiceId(any())).thenReturn(List.of());
            when(merchantCategorizationService.applyRulesToItems(anyList(), any(User.class)))
                .thenReturn(0);

            // When
            invoiceImportService.processImportAsync(1L);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<InvoiceItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
            verify(merchantCategorizationService).applyRulesToItems(itemsCaptor.capture(), any(User.class));

            // Should have at least the 3 new items
            assertThat(itemsCaptor.getValue().size()).isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Auto-categorization Scenario Tests")
    class AutoCategorizationScenarioTests {

        @Test
        @DisplayName("Should auto-categorize items with matching rules")
        void shouldAutoCategorizeItemsWithMatchingRules() throws IOException {
            // This test simulates the full flow where some items get auto-categorized

            // Given
            ParsedInvoiceData parsedData = new ParsedInvoiceData(
                "Test Card",     // creditCardName
                "1234",          // cardNumber
                LocalDate.of(2024, 11, 10), // dueDate
                new BigDecimal("50.00"),    // totalAmount
                List.of(
                    new ParsedInvoiceData.ParsedInvoiceItem(
                        "UBER *TRIP",
                        new BigDecimal("50.00"),
                        LocalDate.of(2024, 10, 15),
                        null, 1, 1, 0.9
                    )
                ),
                "Test Bank",     // bankName
                YearMonth.of(2024, 11), // invoiceMonth
                0.95             // confidence
            );

            // Create a rule that should auto-apply
            MerchantCategoryRule uberRule = MerchantCategoryRule.of(
                testUser, "UBER", "UBER *TRIP", transportCategory);
            uberRule.recordConfirmation();
            uberRule.recordConfirmation(); // Now has 3 confirmations, autoApply = true

            when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(testImport));
            when(invoiceImportRepository.save(any(InvoiceImport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(pdfInvoiceParser.parsePdf(any())).thenReturn(parsedData);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(invoiceRepository.findByCreditCardAndMonth(any(), any()))
                .thenReturn(List.of(testInvoice));
            when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(invoiceImportRepository.findByCreatedInvoiceId(any())).thenReturn(List.of());

            // Simulate that the categorization service will auto-categorize 1 item
            when(merchantCategorizationService.applyRulesToItems(anyList(), any(User.class)))
                .thenAnswer(invocation -> {
                    List<InvoiceItem> items = invocation.getArgument(0);
                    // Simulate auto-categorization
                    for (InvoiceItem item : items) {
                        if (item.getDescription().contains("UBER") && item.getCategory() == null) {
                            item.updateCategory(transportCategory, CategorizationSource.AUTO_RULE, uberRule);
                        }
                    }
                    return 1;
                });

            // When
            invoiceImportService.processImportAsync(1L);

            // Then
            verify(merchantCategorizationService).applyRulesToItems(anyList(), eq(testUser));
        }
    }
}
