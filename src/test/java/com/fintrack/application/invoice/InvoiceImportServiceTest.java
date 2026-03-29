package com.fintrack.application.invoice;

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
import com.fintrack.infrastructure.parsing.PdfInvoiceParser;
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
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private com.fintrack.application.creditcard.MerchantCategorizationService merchantCategorizationService;

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
        testUser = User.createLocalUser("Test User", "test@example.com", "password123", roles);
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
        User otherUser = User.createLocalUser("Other User", "other@example.com", "password123", otherRoles);
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
        assertThat(response.message()).isEqualTo("Pendente");

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
        List<ImportInvoiceResponse> responses =
            invoiceImportService.getUserImportsByStatus(testUser, ImportStatus.PENDING);

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
            new ParsedInvoiceData.ParsedInvoiceItem("Amazon Purchase", BigDecimal.valueOf(99.99),
                LocalDate.now(), null, 1, 1, 0.9),
            new ParsedInvoiceData.ParsedInvoiceItem("Netflix Subscription", BigDecimal.valueOf(29.90),
                LocalDate.now(), null, 1, 1, 0.9)
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
            new ParsedInvoiceData.ParsedInvoiceItem("Amazon Purchase", BigDecimal.valueOf(99.99),
                LocalDate.now(), null, 1, 1, 0.9),
            new ParsedInvoiceData.ParsedInvoiceItem("Netflix Subscription", BigDecimal.valueOf(29.90),
                LocalDate.now(), null, 1, 1, 0.9)
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
            new ParsedInvoiceData.ParsedInvoiceItem("Amazon Purchase", BigDecimal.valueOf(99.99),
                LocalDate.now(), null, 1, 1, 0.9), // Duplicate
            new ParsedInvoiceData.ParsedInvoiceItem("Spotify Premium", BigDecimal.valueOf(19.90),
                LocalDate.now(), null, 1, 1, 0.9), // New
            new ParsedInvoiceData.ParsedInvoiceItem("Netflix Subscription", BigDecimal.valueOf(29.90),
                LocalDate.now(), null, 1, 1, 0.9), // Duplicate
            new ParsedInvoiceData.ParsedInvoiceItem("Uber Ride", BigDecimal.valueOf(25.50),
                LocalDate.now(), null, 1, 1, 0.9) // New
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
            new ParsedInvoiceData.ParsedInvoiceItem("iPhone 15 Pro", BigDecimal.valueOf(500.00),
                LocalDate.now(), null, 1, 12, 0.9), // Duplicate
            new ParsedInvoiceData.ParsedInvoiceItem("iPhone 15 Pro", BigDecimal.valueOf(500.00),
                LocalDate.now(), null, 2, 12, 0.9), // New (different installment)
            new ParsedInvoiceData.ParsedInvoiceItem("iPhone 15 Pro", BigDecimal.valueOf(500.00),
                LocalDate.now(), null, 3, 12, 0.9)  // New (different installment)
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
    void processImportAsync_WithLegitimateDuplicateItems_ShouldImportAll() throws IOException {
        // Given — PDF contains two identical items (e.g., two "BASTARDS" purchases on the same date/amount)
        InvoiceImport importRecord = createTestImportRecord();

        Invoice emptyInvoice = Invoice.of(testCreditCard, YearMonth.now(), LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(emptyInvoice, "id", 1L);

        LocalDate purchaseDate = LocalDate.of(2026, 2, 15);
        List<ParsedInvoiceData.ParsedInvoiceItem> itemsWithLegitDuplicates = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("BASTARDS", BigDecimal.valueOf(20.00),
                purchaseDate, null, 1, 1, 0.9),
            new ParsedInvoiceData.ParsedInvoiceItem("BASTARDS", BigDecimal.valueOf(20.00),
                purchaseDate, null, 1, 1, 0.9),
            new ParsedInvoiceData.ParsedInvoiceItem("Spotify Premium", BigDecimal.valueOf(19.90),
                purchaseDate, null, 1, 1, 0.9)
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(itemsWithLegitDuplicates);

        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of(emptyInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(emptyInvoice);
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of());

        // When
        invoiceImportService.processImportAsync(1L);

        // Then — all 3 items should be imported (both "BASTARDS" are legitimate purchases)
        assertThat(emptyInvoice.getItems()).hasSize(3);
        long bastardsCount = emptyInvoice.getItems().stream()
            .filter(i -> "BASTARDS".equals(i.getDescription()))
            .count();
        assertThat(bastardsCount).isEqualTo(2);
    }

    @Test
    void processImportAsync_WithLegitimateDuplicatesAndExistingItems_ShouldNotReimport() throws IOException {
        // Given — invoice already has two identical "BASTARDS" items, re-importing same PDF should add nothing
        InvoiceImport importRecord = createTestImportRecord();

        Invoice existingInvoice = Invoice.of(testCreditCard, YearMonth.now(), LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(existingInvoice, "id", 1L);

        LocalDate purchaseDate = LocalDate.of(2026, 2, 15);
        InvoiceItem existing1 = InvoiceItem.of(existingInvoice, "BASTARDS", BigDecimal.valueOf(20.00),
            null, purchaseDate, 1, 1);
        InvoiceItem existing2 = InvoiceItem.of(existingInvoice, "BASTARDS", BigDecimal.valueOf(20.00),
            null, purchaseDate, 1, 1);
        ReflectionTestUtils.setField(existing1, "id", 10L);
        ReflectionTestUtils.setField(existing2, "id", 11L);
        existingInvoice.addItem(existing1);
        existingInvoice.addItem(existing2);

        List<ParsedInvoiceData.ParsedInvoiceItem> sameItems = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("BASTARDS", BigDecimal.valueOf(20.00),
                purchaseDate, null, 1, 1, 0.9),
            new ParsedInvoiceData.ParsedInvoiceItem("BASTARDS", BigDecimal.valueOf(20.00),
                purchaseDate, null, 1, 1, 0.9)
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(sameItems);

        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(existingInvoice);
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of());

        // When
        invoiceImportService.processImportAsync(1L);

        // Then — no new items should be added (both already exist)
        assertThat(existingInvoice.getItems()).hasSize(2);
    }

    @Test
    void processImportAsync_WithMoreDuplicatesThanExisting_ShouldAddOnlyExtras() throws IOException {
        // Given — invoice has 1 "BASTARDS", PDF has 3 "BASTARDS" => should add 2 more
        InvoiceImport importRecord = createTestImportRecord();

        Invoice existingInvoice = Invoice.of(testCreditCard, YearMonth.now(), LocalDate.now().plusDays(30));
        ReflectionTestUtils.setField(existingInvoice, "id", 1L);

        LocalDate purchaseDate = LocalDate.of(2026, 2, 15);
        InvoiceItem existing = InvoiceItem.of(existingInvoice, "BASTARDS", BigDecimal.valueOf(20.00),
            null, purchaseDate, 1, 1);
        ReflectionTestUtils.setField(existing, "id", 10L);
        existingInvoice.addItem(existing);

        List<ParsedInvoiceData.ParsedInvoiceItem> threeItems = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("BASTARDS", BigDecimal.valueOf(20.00),
                purchaseDate, null, 1, 1, 0.9),
            new ParsedInvoiceData.ParsedInvoiceItem("BASTARDS", BigDecimal.valueOf(20.00),
                purchaseDate, null, 1, 1, 0.9),
            new ParsedInvoiceData.ParsedInvoiceItem("BASTARDS", BigDecimal.valueOf(20.00),
                purchaseDate, null, 1, 1, 0.9)
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(threeItems);

        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(existingInvoice);
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of());

        // When
        invoiceImportService.processImportAsync(1L);

        // Then — should have 3 total: 1 existing + 2 new
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
    void processImportAsync_WithInvoiceAlreadyReferencedByAnotherImport_ShouldMarkWithoutReference()
        throws IOException {
        // Given
        InvoiceImport importRecord = createTestImportRecord();
        InvoiceImport existingImport = createTestImportRecord();
        ReflectionTestUtils.setField(existingImport, "id", 2L);
        
        List<ParsedInvoiceData.ParsedInvoiceItem> items = List.of(
            new ParsedInvoiceData.ParsedInvoiceItem("New Item", BigDecimal.valueOf(50.00),
                LocalDate.now(), null, 1, 1, 0.9)
        );
        ParsedInvoiceData parsedData = createParsedDataWithItems(items);
        
        Invoice existingInvoice = createExistingInvoiceWithItems();
        
        when(invoiceImportRepository.findById(1L)).thenReturn(Optional.of(importRecord));
        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceRepository.findByCreditCardAndMonth(any(), any())).thenReturn(List.of(existingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(existingInvoice);
        // Existing import found
        when(invoiceImportRepository.findByCreatedInvoiceId(1L)).thenReturn(List.of(existingImport));

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
            new ParsedInvoiceData.ParsedInvoiceItem("New Item", BigDecimal.valueOf(50.00),
                LocalDate.now(), null, 1, 1, 0.9)
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

    // ===== DEDUPLICATION TESTS =====

    @Test
    void shouldUseOptimizedSignatureComputationForSpecialItems() throws Exception {
        // Given
        String iofDescription = "IOF DESPESA NO EXTERIOR";
        BigDecimal amount = BigDecimal.valueOf(0.83);
        LocalDate date1 = LocalDate.of(2025, 8, 23);
        LocalDate date2 = LocalDate.of(2025, 8, 24);

        // When
        String signature1 = invokeComputeItemSignature(iofDescription, amount, date1, 1, 1);
        String signature2 = invokeComputeItemSignature(iofDescription, amount, date2, 1, 1);

        // Then
        assertThat(signature1).isNotEqualTo(signature2); // Different dates should produce different signatures
        assertThat(signature1).isNotEmpty();
        assertThat(signature2).isNotEmpty();
    }

    @Test
    void shouldUseStrictSignatureComputationForRegularItems() throws Exception {
        // Given
        String regularDescription = "Amazon Purchase";
        BigDecimal amount = BigDecimal.valueOf(99.99);
        LocalDate date = LocalDate.of(2025, 8, 23);

        // When
        String signature1 = invokeComputeItemSignature(regularDescription, amount, date, 1, 1);
        // Different installments
        String signature2 = invokeComputeItemSignature(regularDescription, amount, date, 2, 2);

        // Then
        assertThat(signature1).isNotEqualTo(signature2); // Different installments should produce different signatures
    }

    @Test
    void shouldProduceSameSignatureRegardlessOfDatePrefixOrInstallmentSuffix() throws Exception {
        // Given — same item parsed with different description formats
        BigDecimal amount = BigDecimal.valueOf(214.85);
        LocalDate date = LocalDate.of(2025, 2, 15);

        // Old flow: "02/02 MERCADOLIVRE*5PRODUTOS" with installments 1/2
        String oldFlowDesc = "02/02 MERCADOLIVRE*5PRODUTOS";
        // New flow: "MERCADOLIVRE*5PRODUTOS 01/02" with installments 1/1
        String newFlowDesc = "MERCADOLIVRE*5PRODUTOS 01/02";
        // Clean description (no prefix/suffix)
        String cleanDesc = "MERCADOLIVRE*5PRODUTOS";

        // When — compute signatures with SAME installment values (normalization strips from desc)
        String sigOld = invokeComputeItemSignature(oldFlowDesc, amount, date, 1, 2);
        String sigNew = invokeComputeItemSignature(newFlowDesc, amount, date, 1, 2);
        String sigClean = invokeComputeItemSignature(cleanDesc, amount, date, 1, 2);

        // Then — all should match because date prefix/installment suffix are stripped
        assertThat(sigOld).isEqualTo(sigClean);
        assertThat(sigNew).isEqualTo(sigClean);
    }

    @Test
    void shouldNotStripLegitimateDescriptionParts() throws Exception {
        // Given — descriptions that look like but aren't date/installment patterns
        BigDecimal amount = BigDecimal.valueOf(50.00);
        LocalDate date = LocalDate.of(2025, 3, 1);

        // "12/2025 Annual Fee" — not DD/MM pattern (4-digit year)
        String sig1 = invokeComputeItemSignature("12/2025 Annual Fee", amount, date, 1, 1);
        String sig2 = invokeComputeItemSignature("12/2025 annual fee", amount, date, 1, 1);
        assertThat(sig1).isEqualTo(sig2);

        // Different actual descriptions should produce different signatures
        String sigA = invokeComputeItemSignature("STORE ABC", amount, date, 1, 1);
        String sigB = invokeComputeItemSignature("STORE XYZ", amount, date, 1, 1);
        assertThat(sigA).isNotEqualTo(sigB);
    }

    @Test
    void shouldBuildExistingSignatureCountsEfficiently() throws Exception {
        // Given
        Invoice invoice = createExistingInvoiceWithItems();

        // When
        Map<String, Integer> counts = invokeBuildExistingSignatureCounts(invoice);

        // Then
        assertThat(counts).hasSize(2);
        assertThat(counts.values()).allMatch(count -> count >= 1);
    }

    @Test
    void shouldHandleEmptyInvoiceEfficiently() throws Exception {
        // Given
        Invoice invoice = Invoice.of(testCreditCard, YearMonth.now(), LocalDate.now().plusDays(30));

        // When
        Map<String, Integer> counts = invokeBuildExistingSignatureCounts(invoice);

        // Then
        assertThat(counts).isEmpty();
    }

    @Test
    void shouldCreateValidItemAdditionResult() throws Exception {
        // When
        InvoiceImportService.ItemAdditionResult result = new InvoiceImportService.ItemAdditionResult(5, 2);

        // Then
        assertThat(result.added()).isEqualTo(5);
        assertThat(result.skipped()).isEqualTo(2);
        assertThat(result.totalProcessed()).isEqualTo(7);
        assertThat(result.successRate()).isCloseTo(71.43, within(0.01));
    }

    @Test
    void shouldValidateNegativeCountsInItemAdditionResult() {
        // When & Then - Test that creating records with negative values throws exceptions
        assertThatThrownBy(() -> new InvoiceImportService.ItemAdditionResult(-1, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Added count cannot be negative");
        
        assertThatThrownBy(() -> new InvoiceImportService.ItemAdditionResult(0, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Skipped count cannot be negative");
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
        InvoiceItem item1 = InvoiceItem.of(invoice, "Amazon Purchase", BigDecimal.valueOf(99.99),
            null, LocalDate.now(), 1, 1);
        InvoiceItem item2 = InvoiceItem.of(invoice, "Netflix Subscription", BigDecimal.valueOf(29.90),
            null, LocalDate.now(), 1, 1);
        
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
        InvoiceItem item1 = InvoiceItem.of(invoice, "iPhone 15 Pro", BigDecimal.valueOf(500.00),
            null, LocalDate.now(), 1, 12);
        ReflectionTestUtils.setField(item1, "id", 1L);
        
        invoice.addItem(item1);
        
        return invoice;
    }

    // Helper methods for deduplication tests (using reflection)
    private String invokeComputeItemSignature(String description, BigDecimal amount, LocalDate date,
        int installmentNumber, int totalInstallments) throws Exception {
        Method method = InvoiceImportService.class.getDeclaredMethod("computeItemSignature",
            String.class, BigDecimal.class, LocalDate.class, int.class, int.class);
        method.setAccessible(true);
        return (String) method.invoke(invoiceImportService, description, amount, date,
            installmentNumber, totalInstallments);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> invokeBuildExistingSignatureCounts(Invoice invoice) throws Exception {
        Method method = InvoiceImportService.class.getDeclaredMethod("buildExistingSignatureCounts", Invoice.class);
        method.setAccessible(true);
        return (Map<String, Integer>) method.invoke(invoiceImportService, invoice);
    }
} 