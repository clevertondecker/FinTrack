package com.fintrack.service.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.invoice.InvoiceImport;
import com.fintrack.domain.invoice.ImportSource;
import com.fintrack.domain.invoice.ImportStatus;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.invoice.ImportInvoiceRequest;
import com.fintrack.dto.invoice.ImportInvoiceResponse;
import com.fintrack.dto.invoice.ImportProgressResponse;
import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceJpaRepository;
import com.fintrack.infrastructure.persistence.invoice.InvoiceImportJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceImportService.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceImportServiceTest {

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
    private MockMultipartFile testFile;
    private ImportInvoiceRequest testRequest;

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        testUser = User.of("Test User", "test@example.com", "password123", roles);
        testBank = Bank.of("001", "Test Bank");
        testCreditCard = CreditCard.of("Test Card", "1234", BigDecimal.valueOf(10000), testUser, testBank);
        testFile = new MockMultipartFile(
            "file", 
            "test-invoice.pdf", 
            "application/pdf", 
            "test content".getBytes()
        );
        testRequest = new ImportInvoiceRequest(1L);
    }

    @Test
    void importInvoice_WithValidData_ShouldCreateImportAndStartProcessing() throws IOException {
        // Given
        when(creditCardRepository.findById(1L)).thenReturn(Optional.of(testCreditCard));
        when(invoiceImportRepository.save(any(InvoiceImport.class))).thenAnswer(invocation -> {
            InvoiceImport importRecord = invocation.getArgument(0);
            ReflectionTestUtils.setField(importRecord, "id", 1L);
            return importRecord;
        });

        // When
        ImportInvoiceResponse response = invoiceImportService.importInvoice(testFile, testRequest, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.importId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ImportStatus.PENDING);
        assertThat(response.source()).isEqualTo(ImportSource.PDF);
        assertThat(response.originalFileName()).isEqualTo("test-invoice.pdf");
        assertThat(response.message()).isEqualTo("Import iniciado com sucesso. Processando em background.");

        verify(creditCardRepository).findById(1L);
        verify(invoiceImportRepository, atLeastOnce()).save(any(InvoiceImport.class));
    }

    @Test
    void importInvoice_WithInvalidCreditCard_ShouldThrowException() {
        // Given
        when(creditCardRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            invoiceImportService.importInvoice(testFile, testRequest, testUser)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("Credit card not found.");

        verify(creditCardRepository).findById(1L);
        verify(invoiceImportRepository, never()).save(any());
    }

    @Test
    void importInvoice_WithCreditCardNotBelongingToUser_ShouldThrowException() {
        // Given
        Set<Role> otherRoles = new HashSet<>();
        otherRoles.add(Role.USER);
        User otherUser = User.of("Other User", "other@example.com", "password123", otherRoles);
        CreditCard otherUserCard = CreditCard.of("Other Card", "5678", BigDecimal.valueOf(5000), otherUser, testBank);
        
        // Set IDs via reflection to ensure proper equality comparison
        try {
            java.lang.reflect.Field userIdField = User.class.getDeclaredField("id");
            userIdField.setAccessible(true);
            userIdField.set(testUser, 1L);
            userIdField.set(otherUser, 2L);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user IDs", e);
        }
        
        when(creditCardRepository.findById(1L)).thenReturn(Optional.of(otherUserCard));

        // When & Then
        assertThatThrownBy(() -> 
            invoiceImportService.importInvoice(testFile, testRequest, testUser)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("Credit card does not belong to user.");

        verify(creditCardRepository).findById(1L);
        verify(invoiceImportRepository, never()).save(any());
    }

    @Test
    void getImportProgress_WithValidImport_ShouldReturnProgress() {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        when(invoiceImportRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(importRecord));

        // When
        ImportProgressResponse response = invoiceImportService.getImportProgress(1L, testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.importId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ImportStatus.PENDING);
        assertThat(response.message()).isEqualTo("Aguardando processamento");

        verify(invoiceImportRepository).findByIdAndUser(1L, testUser);
    }

    @Test
    void getImportProgress_WithImportNotFound_ShouldThrowException() {
        // Given
        when(invoiceImportRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> 
            invoiceImportService.getImportProgress(1L, testUser)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessage("Import not found or access denied.");

        verify(invoiceImportRepository).findByIdAndUser(1L, testUser);
    }

    @Test
    void getUserImports_ShouldReturnUserImports() {
        // Given
        List<InvoiceImport> imports = List.of(createTestImportRecord());
        when(invoiceImportRepository.findByUserOrderByImportedAtDesc(testUser)).thenReturn(imports);

        // When
        List<ImportInvoiceResponse> responses = invoiceImportService.getUserImports(testUser);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).importId()).isEqualTo(1L);

        verify(invoiceImportRepository).findByUserOrderByImportedAtDesc(testUser);
    }

    @Test
    void getUserImportsByStatus_ShouldReturnFilteredImports() {
        // Given
        List<InvoiceImport> imports = List.of(createTestImportRecord());
        when(invoiceImportRepository.findByUserAndStatusOrderByImportedAtDesc(testUser, ImportStatus.PENDING))
            .thenReturn(imports);

        // When
        List<ImportInvoiceResponse> responses = invoiceImportService.getUserImportsByStatus(testUser, ImportStatus.PENDING);

        // Then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).status()).isEqualTo(ImportStatus.PENDING);

        verify(invoiceImportRepository).findByUserAndStatusOrderByImportedAtDesc(testUser, ImportStatus.PENDING);
    }

    @Test
    void processImportAsync_WithSuccessfulParsing_ShouldMarkAsCompleted() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        ParsedInvoiceData parsedData = createTestParsedData();
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
        verify(pdfInvoiceParser).parsePdf(anyString());
        verify(objectMapper).writeValueAsString(any());
    }

    @Test
    void processImportAsync_WithLowConfidence_ShouldMarkForManualReview() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        ParsedInvoiceData parsedData = createTestParsedDataWithLowConfidence();
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
        verify(pdfInvoiceParser).parsePdf(anyString());
        verify(objectMapper).writeValueAsString(any());
    }

    @Test
    void processImportAsync_WithParsingError_ShouldMarkAsFailed() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenThrow(new IOException("PDF parsing failed"));

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
        verify(pdfInvoiceParser).parsePdf(anyString());
    }

    // Helper methods
    private InvoiceImport createTestImportRecord() {
        InvoiceImport importRecord = InvoiceImport.of(testUser, ImportSource.PDF, "test.pdf", "/path/to/file");
        ReflectionTestUtils.setField(importRecord, "id", 1L);
        importRecord.setCreditCard(testCreditCard);
        return importRecord;
    }

    private ParsedInvoiceData createTestParsedData() {
        return new ParsedInvoiceData(
            "Test Card",
            "1234",
            LocalDate.now().plusDays(30),
            BigDecimal.valueOf(1500.00),
            List.of(),
            "Test Bank",
            null,
            0.85
        );
    }

    private ParsedInvoiceData createTestParsedDataWithLowConfidence() {
        return new ParsedInvoiceData(
            "Test Card",
            "1234",
            LocalDate.now().plusDays(30),
            BigDecimal.valueOf(1500.00),
            List.of(),
            "Test Bank",
            null,
            0.5
        );
    }

    // ========== DEDUPLICATION TESTS ==========
    @Test
    void processImportAsync_WithNewInvoiceAndItems_ShouldCreateInvoiceAndAddAllItems() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        List<ParsedInvoiceData.ParsedInvoiceItem> items = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("Amazon Purchase", BigDecimal.valueOf(99.99), LocalDate.now(), null, 1, 1, 0.9),
            new ParsedInvoiceData.ParsedInvoiceItem("Netflix Subscription", BigDecimal.valueOf(29.90), LocalDate.now(), null, 1, 1, 0.9)
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(items);
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", 1L);
            return invoice;
        });
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of()); // No existing imports

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceRepository).findByCreditCardAndMonth(any(), any());
        verify(invoiceRepository, times(2)).save(any(Invoice.class)); // Once for creation, once after adding items
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
        verify(invoiceImportRepository).findByCreatedInvoiceId(1L);
    }

    @Test
    void processImportAsync_WithExistingInvoiceAndDuplicateItems_ShouldSkipDuplicates() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        
        // Create existing invoice with items
        Invoice existingInvoice = createExistingInvoiceWithItems();
        
        // Same items as existing (should be skipped)
        List<ParsedInvoiceData.ParsedInvoiceItem> duplicateItems = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("Amazon Purchase", BigDecimal.valueOf(99.99), LocalDate.now(), null, 1, 1, 0.9),
            new ParsedInvoiceData.ParsedInvoiceItem("Netflix Subscription", BigDecimal.valueOf(29.90), LocalDate.now(), null, 1, 1, 0.9)
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(duplicateItems);
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(existingInvoice);
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of()); // No existing imports

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceRepository).findByCreditCardAndMonth(any(), any());
        verify(invoiceRepository).save(any(Invoice.class)); // Should save even with no new items (for consistency)
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
        verify(invoiceImportRepository).findByCreatedInvoiceId(1L);
        
        // Verify no new items were added (existing invoice should still have 2 items)
        assertThat(existingInvoice.getItems()).hasSize(2);
    }

    @Test
    void processImportAsync_WithExistingInvoiceAndMixedItems_ShouldAddOnlyNewItems() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        
        // Create existing invoice with some items
        Invoice existingInvoice = createExistingInvoiceWithItems();
        
        // Mix of existing and new items
        List<ParsedInvoiceData.ParsedInvoiceItem> mixedItems = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("Amazon Purchase", BigDecimal.valueOf(99.99), LocalDate.now(), null, 1, 1, 0.9), // Duplicate
            new ParsedInvoiceData.ParsedInvoiceItem("Spotify Premium", BigDecimal.valueOf(19.90), LocalDate.now(), null, 1, 1, 0.9), // New
            new ParsedInvoiceData.ParsedInvoiceItem("Netflix Subscription", BigDecimal.valueOf(29.90), LocalDate.now(), null, 1, 1, 0.9), // Duplicate
            new ParsedInvoiceData.ParsedInvoiceItem("Uber Ride", BigDecimal.valueOf(25.50), LocalDate.now(), null, 1, 1, 0.9) // New
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(mixedItems);
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(existingInvoice);
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of()); // No existing imports

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceRepository).findByCreditCardAndMonth(any(), any());
        verify(invoiceRepository).save(any(Invoice.class));
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
        verify(invoiceImportRepository).findByCreatedInvoiceId(1L);
        
        // Verify 2 new items were added (total should be 4: 2 existing + 2 new)
        assertThat(existingInvoice.getItems()).hasSize(4);
    }

    @Test
    void processImportAsync_WithSameItemDifferentInstallments_ShouldAddBothItems() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        
        // Create existing invoice with installment item
        Invoice existingInvoice = createInvoiceWithInstallmentItems();
        
        // Same purchase but different installment numbers (should not be duplicates)
        List<ParsedInvoiceData.ParsedInvoiceItem> installmentItems = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("iPhone 15 Pro", BigDecimal.valueOf(500.00), LocalDate.now(), null, 1, 12, 0.9), // Duplicate
            new ParsedInvoiceData.ParsedInvoiceItem("iPhone 15 Pro", BigDecimal.valueOf(500.00), LocalDate.now(), null, 2, 12, 0.9), // New (different installment)
            new ParsedInvoiceData.ParsedInvoiceItem("iPhone 15 Pro", BigDecimal.valueOf(500.00), LocalDate.now(), null, 3, 12, 0.9)  // New (different installment)
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(installmentItems);
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(existingInvoice);
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of()); // No existing imports

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceRepository).save(any(Invoice.class));
        verify(invoiceImportRepository).findByCreatedInvoiceId(1L);
        
        // Should have 3 items total: 1 existing + 2 new installments
        assertThat(existingInvoice.getItems()).hasSize(3);
    }

    @Test
    void processImportAsync_WithEmptyItemsList_ShouldHandleGracefully() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        ParsedInvoiceData parsedData = createParsedDataWithItems(List.of()); // Empty items
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", 1L);
            return invoice;
        });

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceRepository, times(2)).save(any(Invoice.class)); // Once for creation, once after processing
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
    }

    @Test
    void processImportAsync_WithNullItemsList_ShouldHandleGracefully() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        ParsedInvoiceData parsedData = createParsedDataWithItems(null); // Null items
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", 1L);
            return invoice;
        });
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of()); // No existing imports

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceRepository, times(2)).save(any(Invoice.class)); // Once for creation, once after processing
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
        verify(invoiceImportRepository).findByCreatedInvoiceId(1L);
    }

    @Test
    void processImportAsync_WithInvoiceAlreadyReferencedByAnotherImport_ShouldMarkWithoutReference() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        InvoiceImport existingImport = createTestImportRecord();
        ReflectionTestUtils.setField(existingImport, "id", 2L);
        
        List<ParsedInvoiceData.ParsedInvoiceItem> items = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("New Item", BigDecimal.valueOf(50.00), LocalDate.now(), null, 1, 1, 0.9)
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(items);
        
        Invoice existingInvoice = createExistingInvoiceWithItems();
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(existingInvoice);
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of(existingImport)); // Existing import found

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceImportRepository).findByCreatedInvoiceId(1L);
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
        // Verify the import was marked as completed without invoice reference
        assertThat(importRecord.getStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(importRecord.getCreatedInvoice()).isNull(); // Should not reference the invoice
    }

    @Test
    void processImportAsync_WithNoExistingImportForInvoice_ShouldMarkWithReference() throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        List<ParsedInvoiceData.ParsedInvoiceItem> items = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("New Item", BigDecimal.valueOf(50.00), LocalDate.now(), null, 1, 1, 0.9)
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(items);
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", 1L);
            return invoice;
        });
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of()); // No existing imports

        // When
        invoiceImportService.processImportAsync(1L);

        // Then
        verify(invoiceImportRepository).findByCreatedInvoiceId(1L);
        verify(invoiceImportRepository, times(2)).save(any(InvoiceImport.class));
        // Verify the import was marked as completed with invoice reference
        assertThat(importRecord.getStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(importRecord.getCreatedInvoice()).isNotNull(); // Should reference the invoice
    }

    // Helper methods for deduplication tests
    private ParsedInvoiceData createParsedDataWithItems(List<ParsedInvoiceData.ParsedInvoiceItem> items) {
        return new ParsedInvoiceData(
            "Test Card",
            "1234",
            LocalDate.now().plusDays(30),
            BigDecimal.valueOf(1500.00),
            items,
            "Test Bank",
            null,
            0.85
        );
    }

    private Invoice createExistingInvoiceWithItems() {
        Invoice invoice = Invoice.of(testCreditCard, YearMonth.now(), LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(invoice, "id", 1L);
        
        // Add existing items
        InvoiceItem item1 = InvoiceItem.of(invoice, "Amazon Purchase", BigDecimal.valueOf(99.99), null, LocalDate.now(), 1, 1);
        InvoiceItem item2 = InvoiceItem.of(invoice, "Netflix Subscription", BigDecimal.valueOf(29.90), null, LocalDate.now(), 1, 1);
        
        ReflectionTestUtils.setField(item1, "id", 1L);
        ReflectionTestUtils.setField(item2, "id", 2L);
        
        invoice.addItem(item1);
        invoice.addItem(item2);
        
        return invoice;
    }

    private Invoice createInvoiceWithInstallmentItems() {
        Invoice invoice = Invoice.of(testCreditCard, YearMonth.now(), LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(invoice, "id", 1L);
        
        // Add existing installment item
        InvoiceItem item1 = InvoiceItem.of(invoice, "iPhone 15 Pro", BigDecimal.valueOf(500.00), null, LocalDate.now(), 1, 12);
        ReflectionTestUtils.setField(item1, "id", 1L);
        
        invoice.addItem(item1);
        
        return invoice;
    }
} 