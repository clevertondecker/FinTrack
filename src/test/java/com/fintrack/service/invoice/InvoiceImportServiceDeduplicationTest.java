package com.fintrack.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceJpaRepository;
import com.fintrack.infrastructure.persistence.invoice.InvoiceImportJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for InvoiceImportService deduplication functionality.
 * Tests private methods using reflection to ensure signature computation works correctly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceImportService Deduplication Tests")
class InvoiceImportServiceDeduplicationTest {

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

    @InjectMocks
    private InvoiceImportService invoiceImportService;

    private User testUser;
    private CreditCard testCreditCard;
    private Bank testBank;

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        testUser = User.createLocalUser("Test User", "test@example.com", "password123", roles);
        testBank = Bank.of("001", "Test Bank");
        testCreditCard = CreditCard.of("Test Card", "1234", BigDecimal.valueOf(10000), testUser, testBank);
    }

    @Nested
    @DisplayName("Item Signature Computation")
    class ItemSignatureComputationTests {

        @Test
        @DisplayName("Should generate same signature for identical items")
        void shouldGenerateSameSignatureForIdenticalItems() throws Exception {
            // Given
            String description = "Amazon Purchase";
            BigDecimal amount = BigDecimal.valueOf(99.99);
            LocalDate purchaseDate = LocalDate.of(2024, 1, 15);
            int installments = 1;
            int totalInstallments = 1;

            // When
            String signature1 = invokeComputeItemSignature(description, amount, purchaseDate, installments, totalInstallments);
            String signature2 = invokeComputeItemSignature(description, amount, purchaseDate, installments, totalInstallments);

            // Then
            assertThat(signature1).isEqualTo(signature2);
            assertThat(signature1).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate different signatures for different descriptions")
        void shouldGenerateDifferentSignaturesForDifferentDescriptions() throws Exception {
            // Given
            BigDecimal amount = BigDecimal.valueOf(99.99);
            LocalDate purchaseDate = LocalDate.of(2024, 1, 15);

            // When
            String signature1 = invokeComputeItemSignature("Amazon Purchase", amount, purchaseDate, 1, 1);
            String signature2 = invokeComputeItemSignature("Netflix Subscription", amount, purchaseDate, 1, 1);

            // Then
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("Should generate different signatures for different amounts")
        void shouldGenerateDifferentSignaturesForDifferentAmounts() throws Exception {
            // Given
            String description = "Amazon Purchase";
            LocalDate purchaseDate = LocalDate.of(2024, 1, 15);

            // When
            String signature1 = invokeComputeItemSignature(description, BigDecimal.valueOf(99.99), purchaseDate, 1, 1);
            String signature2 = invokeComputeItemSignature(description, BigDecimal.valueOf(199.99), purchaseDate, 1, 1);

            // Then
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("Should generate different signatures for different installments")
        void shouldGenerateDifferentSignaturesForDifferentInstallments() throws Exception {
            // Given
            String description = "iPhone 15 Pro";
            BigDecimal amount = BigDecimal.valueOf(500.00);
            LocalDate purchaseDate = LocalDate.of(2024, 1, 15);

            // When
            String signature1 = invokeComputeItemSignature(description, amount, purchaseDate, 1, 12);
            String signature2 = invokeComputeItemSignature(description, amount, purchaseDate, 2, 12);

            // Then
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("Should normalize description properly")
        void shouldNormalizeDescriptionProperly() throws Exception {
            // Given
            BigDecimal amount = BigDecimal.valueOf(99.99);
            LocalDate purchaseDate = LocalDate.of(2024, 1, 15);

            // When - different cases and whitespace should produce same signature
            String signature1 = invokeComputeItemSignature("AMAZON   PURCHASE", amount, purchaseDate, 1, 1);
            String signature2 = invokeComputeItemSignature("amazon purchase", amount, purchaseDate, 1, 1);
            String signature3 = invokeComputeItemSignature("  Amazon    Purchase  ", amount, purchaseDate, 1, 1);

            // Then
            assertThat(signature1).isEqualTo(signature2).isEqualTo(signature3);
        }

        @Test
        @DisplayName("Should handle null values gracefully")
        void shouldHandleNullValuesGracefully() throws Exception {
            // When/Then - should not throw exceptions
            String signature1 = invokeComputeItemSignature(null, null, null, 1, 1);
            String signature2 = invokeComputeItemSignature("", BigDecimal.ZERO, LocalDate.now(), 1, 1);

            assertThat(signature1).isNotEmpty();
            assertThat(signature2).isNotEmpty();
            assertThat(signature1).isNotEqualTo(signature2);
        }

        @Test
        @DisplayName("Should normalize amount to 2 decimal places")
        void shouldNormalizeAmountTo2DecimalPlaces() throws Exception {
            // Given
            String description = "Test Item";
            LocalDate purchaseDate = LocalDate.of(2024, 1, 15);

            // When - different decimal precision should produce same signature
            String signature1 = invokeComputeItemSignature(description, new BigDecimal("99.9"), purchaseDate, 1, 1);
            String signature2 = invokeComputeItemSignature(description, new BigDecimal("99.90"), purchaseDate, 1, 1);
            String signature3 = invokeComputeItemSignature(description, new BigDecimal("99.900"), purchaseDate, 1, 1);

            // Then
            assertThat(signature1).isEqualTo(signature2).isEqualTo(signature3);
        }
    }

    @Nested
    @DisplayName("ParsedInvoiceItem Signature Computation")
    class ParsedItemSignatureTests {

        @Test
        @DisplayName("Should compute signature from ParsedInvoiceItem")
        void shouldComputeSignatureFromParsedItem() throws Exception {
            // Given
            ParsedInvoiceData.ParsedInvoiceItem parsedItem = new ParsedInvoiceData.ParsedInvoiceItem(
                "Amazon Purchase",
                BigDecimal.valueOf(99.99),
                LocalDate.of(2024, 1, 15),
                null,
                1,
                1,
                0.9
            );

            // When
            String signature = invokeComputeItemSignatureFromParsedItem(parsedItem);

            // Then
            assertThat(signature).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle ParsedInvoiceItem with null values")
        void shouldHandleParsedItemWithNullValues() throws Exception {
            // Given
            ParsedInvoiceData.ParsedInvoiceItem parsedItem = new ParsedInvoiceData.ParsedInvoiceItem(
                "Test Item",
                BigDecimal.valueOf(50.00),
                null, // null date
                null, // null category
                null, // null installments
                null, // null total installments
                0.8   // confidence
            );

            // When
            String signature = invokeComputeItemSignatureFromParsedItem(parsedItem);

            // Then
            assertThat(signature).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Description Normalization")
    class DescriptionNormalizationTests {

        @Test
        @DisplayName("Should normalize description correctly")
        void shouldNormalizeDescriptionCorrectly() throws Exception {
            // When
            String result1 = invokeNormalizeDescription("  AMAZON   PURCHASE  ");
            String result2 = invokeNormalizeDescription("amazon purchase");
            String result3 = invokeNormalizeDescription("Amazon\t\tPurchase");

            // Then
            assertThat(result1).isEqualTo("amazon purchase");
            assertThat(result2).isEqualTo("amazon purchase");
            assertThat(result3).isEqualTo("amazon purchase");
        }

        @Test
        @DisplayName("Should handle null and empty descriptions")
        void shouldHandleNullAndEmptyDescriptions() throws Exception {
            // When
            String nullResult = invokeNormalizeDescription(null);
            String emptyResult = invokeNormalizeDescription("");
            String blankResult = invokeNormalizeDescription("   ");

            // Then
            assertThat(nullResult).isEqualTo("");
            assertThat(emptyResult).isEqualTo("");
            assertThat(blankResult).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Amount Normalization")
    class AmountNormalizationTests {

        @Test
        @DisplayName("Should normalize amount to 2 decimal places")
        void shouldNormalizeAmountTo2DecimalPlaces() throws Exception {
            // When
            String result1 = invokeNormalizeAmount(new BigDecimal("99.9"));
            String result2 = invokeNormalizeAmount(new BigDecimal("99.90"));
            String result3 = invokeNormalizeAmount(new BigDecimal("99.999"));

            // Then
            assertThat(result1).isEqualTo("99.90");
            assertThat(result2).isEqualTo("99.90");
            assertThat(result3).isEqualTo("100.00"); // Rounded up
        }

        @Test
        @DisplayName("Should handle null amount")
        void shouldHandleNullAmount() throws Exception {
            // When
            String result = invokeNormalizeAmount(null);

            // Then
            assertThat(result).isEqualTo("0.00");
        }
    }

    @Nested
    @DisplayName("Date and Installment Resolution")
    class DateAndInstallmentResolutionTests {

        @Test
        @DisplayName("Should resolve date correctly")
        void shouldResolveDateCorrectly() throws Exception {
            // Given
            LocalDate specificDate = LocalDate.of(2024, 1, 15);

            // When
            LocalDate resolvedDate = invokeResolveDate(specificDate);
            LocalDate resolvedNullDate = invokeResolveDate(null);

            // Then
            assertThat(resolvedDate).isEqualTo(specificDate);
            assertThat(resolvedNullDate).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Should resolve installments correctly")
        void shouldResolveInstallmentsCorrectly() throws Exception {
            // When
            int resolved1 = invokeResolveInstallments(5);
            int resolved2 = invokeResolveInstallments(null);

            // Then
            assertThat(resolved1).isEqualTo(5);
            assertThat(resolved2).isEqualTo(1); // DEFAULT_INSTALLMENTS
        }
    }

    @Nested
    @DisplayName("SHA-256 Hashing")
    class SHA256HashingTests {

        @Test
        @DisplayName("Should generate consistent SHA-256 hash")
        void shouldGenerateConsistentSHA256Hash() throws Exception {
            // Given
            String input = "test input";

            // When
            String hash1 = invokeSha256(input);
            String hash2 = invokeSha256(input);

            // Then
            assertThat(hash1).isEqualTo(hash2);
            assertThat(hash1).hasSize(64); // SHA-256 produces 64-character hex string
            assertThat(hash1).matches("^[a-f0-9]{64}$"); // Only lowercase hex characters
        }

        @Test
        @DisplayName("Should generate different hashes for different inputs")
        void shouldGenerateDifferentHashesForDifferentInputs() throws Exception {
            // When
            String hash1 = invokeSha256("input1");
            String hash2 = invokeSha256("input2");

            // Then
            assertThat(hash1).isNotEqualTo(hash2);
        }
    }

    // Helper methods to invoke private methods using reflection
    private String invokeComputeItemSignature(String description, BigDecimal amount, LocalDate purchaseDate, 
                                            int installments, int totalInstallments) throws Exception {
        Method method = InvoiceImportService.class.getDeclaredMethod("computeItemSignature", 
            String.class, BigDecimal.class, LocalDate.class, int.class, int.class);
        method.setAccessible(true);
        return (String) method.invoke(invoiceImportService, description, amount, purchaseDate, installments, totalInstallments);
    }

    private String invokeComputeItemSignatureFromParsedItem(ParsedInvoiceData.ParsedInvoiceItem parsedItem) throws Exception {
        Method method = InvoiceImportService.class.getDeclaredMethod("computeItemSignature", 
            ParsedInvoiceData.ParsedInvoiceItem.class);
        method.setAccessible(true);
        return (String) method.invoke(invoiceImportService, parsedItem);
    }

    private String invokeNormalizeDescription(String description) throws Exception {
        Method method = InvoiceImportService.class.getDeclaredMethod("normalizeDescription", String.class);
        method.setAccessible(true);
        return (String) method.invoke(invoiceImportService, description);
    }

    private String invokeNormalizeAmount(BigDecimal amount) throws Exception {
        Method method = InvoiceImportService.class.getDeclaredMethod("normalizeAmount", BigDecimal.class);
        method.setAccessible(true);
        return (String) method.invoke(invoiceImportService, amount);
    }

    private LocalDate invokeResolveDate(LocalDate date) throws Exception {
        Method method = InvoiceImportService.class.getDeclaredMethod("resolveDate", LocalDate.class);
        method.setAccessible(true);
        return (LocalDate) method.invoke(invoiceImportService, date);
    }

    private int invokeResolveInstallments(Integer installments) throws Exception {
        Method method = InvoiceImportService.class.getDeclaredMethod("resolveInstallments", Integer.class);
        method.setAccessible(true);
        return (int) method.invoke(invoiceImportService, installments);
    }

    private String invokeSha256(String input) throws Exception {
        Method method = InvoiceImportService.class.getDeclaredMethod("sha256", String.class);
        method.setAccessible(true);
        return (String) method.invoke(invoiceImportService, input);
    }
}