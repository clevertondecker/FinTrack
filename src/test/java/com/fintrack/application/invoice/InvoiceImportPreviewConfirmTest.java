package com.fintrack.application.invoice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.application.creditcard.MerchantCategorizationService;
import com.fintrack.domain.creditcard.Bank;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.invoice.InvoiceImport;
import com.fintrack.domain.invoice.ImportSource;
import com.fintrack.domain.user.Role;
import com.fintrack.domain.user.User;
import com.fintrack.dto.invoice.ConfirmImportRequest;
import com.fintrack.dto.invoice.ConfirmImportResponse;
import com.fintrack.dto.invoice.DetectedCardMapping;
import com.fintrack.dto.invoice.ImportPreviewResponse;
import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedCardSection;
import com.fintrack.dto.invoice.ParsedInvoiceData.ParsedInvoiceItem;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceImportPreviewConfirmTest {

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
    @Mock
    private CardAutoMatchService cardAutoMatchService;
    @Mock
    private com.fintrack.application.creditcard.InstallmentProjectionService installmentProjectionService;

    @InjectMocks
    private InvoiceImportService service;

    private User testUser;
    private Bank testBank;
    private CreditCard card1234;
    private CreditCard card5678;

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        testUser = User.createLocalUser("Test User", "test@example.com", "password123", roles);
        testBank = Bank.of("001", "Test Bank");
        card1234 = CreditCard.of("Main Card", "1234", BigDecimal.valueOf(10000), testUser, testBank);
        ReflectionTestUtils.setField(card1234, "id", 1L);
        card5678 = CreditCard.of("Additional Card", "5678", BigDecimal.valueOf(5000), testUser, testBank);
        ReflectionTestUtils.setField(card5678, "id", 2L);
    }

    @Test
    void previewImport_withMultipleCards_shouldReturnDetectedMappings() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", "pdf content".getBytes());

        ParsedInvoiceData parsedData = new ParsedInvoiceData(
                "Main Card", "1234",
                LocalDate.of(2024, 11, 10), new BigDecimal("150.00"),
                List.of(),
                "Santander", YearMonth.of(2024, 11), 0.9,
                List.of(
                        new ParsedCardSection("1234", "CARTÃO FINAL 1234",
                                List.of(new ParsedInvoiceItem("UBER", new BigDecimal("50.00"),
                                        LocalDate.of(2024, 10, 5), null, 1, 1, 0.9)),
                                new BigDecimal("50.00")),
                        new ParsedCardSection("5678", "CARTÃO FINAL 5678",
                                List.of(new ParsedInvoiceItem("AMAZON", new BigDecimal("100.00"),
                                        LocalDate.of(2024, 10, 7), null, 1, 1, 0.9)),
                                new BigDecimal("100.00"))
                ));

        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceImportRepository.save(any(InvoiceImport.class))).thenAnswer(inv -> {
            InvoiceImport record = inv.getArgument(0);
            ReflectionTestUtils.setField(record, "id", 10L);
            return record;
        });
        List<DetectedCardMapping> detectedMappings = List.of(
                new DetectedCardMapping("1234", "CARTÃO FINAL 1234", 1L, "Main Card",
                        true, false, List.of(1L),
                        List.of(new ParsedInvoiceItem("UBER", new BigDecimal("50.00"),
                                LocalDate.of(2024, 10, 5), null, 1, 1, 0.9)),
                        new BigDecimal("50.00")),
                new DetectedCardMapping("5678", "CARTÃO FINAL 5678", 2L, "Additional Card",
                        true, false, List.of(2L),
                        List.of(new ParsedInvoiceItem("AMAZON", new BigDecimal("100.00"),
                                LocalDate.of(2024, 10, 7), null, 1, 1, 0.9)),
                        new BigDecimal("100.00"))
        );
        when(cardAutoMatchService.buildDetectedCardMappings(any(ParsedInvoiceData.class), eq(testUser)))
                .thenReturn(detectedMappings);

        ImportPreviewResponse response = service.previewImport(file, testUser);

        assertThat(response.importId()).isEqualTo(10L);
        assertThat(response.detectedCards()).hasSize(2);
        assertThat(response.allCardsMatched()).isTrue();

        assertThat(response.detectedCards().get(0).detectedLastFourDigits()).isEqualTo("1234");
        assertThat(response.detectedCards().get(0).matchedCreditCardId()).isEqualTo(1L);
        assertThat(response.detectedCards().get(0).autoMatched()).isTrue();

        assertThat(response.detectedCards().get(1).detectedLastFourDigits()).isEqualTo("5678");
        assertThat(response.detectedCards().get(1).matchedCreditCardId()).isEqualTo(2L);
        assertThat(response.detectedCards().get(1).autoMatched()).isTrue();
    }

    @Test
    void previewImport_withUnmatchedCard_shouldFlagAsNotMatched() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", "pdf content".getBytes());

        ParsedInvoiceData parsedData = new ParsedInvoiceData(
                null, "9999",
                LocalDate.of(2024, 11, 10), new BigDecimal("50.00"),
                List.of(new ParsedInvoiceItem("UBER", new BigDecimal("50.00"),
                        LocalDate.of(2024, 10, 5), null, 1, 1, 0.9)),
                "Santander", YearMonth.of(2024, 11), 0.9,
                List.of(new ParsedCardSection("9999", "CARTÃO FINAL 9999",
                        List.of(new ParsedInvoiceItem("UBER", new BigDecimal("50.00"),
                                LocalDate.of(2024, 10, 5), null, 1, 1, 0.9)),
                        new BigDecimal("50.00")))
        );

        when(pdfInvoiceParser.parsePdf(anyString())).thenReturn(parsedData);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(invoiceImportRepository.save(any(InvoiceImport.class))).thenAnswer(inv -> {
            InvoiceImport record = inv.getArgument(0);
            ReflectionTestUtils.setField(record, "id", 11L);
            return record;
        });
        List<DetectedCardMapping> detectedMappings = List.of(
                new DetectedCardMapping("9999", "CARTÃO FINAL 9999", null, null,
                        false, false, List.of(),
                        List.of(new ParsedInvoiceItem("UBER", new BigDecimal("50.00"),
                                LocalDate.of(2024, 10, 5), null, 1, 1, 0.9)),
                        new BigDecimal("50.00"))
        );
        when(cardAutoMatchService.buildDetectedCardMappings(any(ParsedInvoiceData.class), eq(testUser)))
                .thenReturn(detectedMappings);

        ImportPreviewResponse response = service.previewImport(file, testUser);

        assertThat(response.allCardsMatched()).isFalse();
        assertThat(response.detectedCards().get(0).autoMatched()).isFalse();
        assertThat(response.detectedCards().get(0).matchedCreditCardId()).isNull();
    }

    @Test
    void confirmImport_withValidMappings_shouldCreateInvoicesPerCard() throws JsonProcessingException {
        InvoiceImport importRecord = InvoiceImport.of(testUser, ImportSource.PDF, "test.pdf", "/tmp/test.pdf");
        ReflectionTestUtils.setField(importRecord, "id", 10L);
        importRecord.markAsPendingReview();
        importRecord.setParsedData("{}");

        ParsedInvoiceData parsedData = new ParsedInvoiceData(
                null, null,
                LocalDate.of(2024, 11, 10), new BigDecimal("150.00"),
                List.of(),
                "Santander", YearMonth.of(2024, 11), 0.9,
                List.of(
                        new ParsedCardSection("1234", "CARTÃO FINAL 1234",
                                List.of(new ParsedInvoiceItem("UBER", new BigDecimal("50.00"),
                                        LocalDate.of(2024, 10, 5), null, 1, 1, 0.9)),
                                new BigDecimal("50.00")),
                        new ParsedCardSection("5678", "CARTÃO FINAL 5678",
                                List.of(new ParsedInvoiceItem("AMAZON", new BigDecimal("100.00"),
                                        LocalDate.of(2024, 10, 7), null, 1, 1, 0.9)),
                                new BigDecimal("100.00"))
                ));

        when(invoiceImportRepository.findByIdAndUser(10L, testUser))
                .thenReturn(Optional.of(importRecord));
        when(objectMapper.readValue(eq("{}"), eq(ParsedInvoiceData.class)))
                .thenReturn(parsedData);
        when(cardAutoMatchService.buildItemsByCardMap(parsedData))
                .thenReturn(Map.of(
                        "1234", List.of(new ParsedInvoiceItem("UBER", new BigDecimal("50.00"),
                                LocalDate.of(2024, 10, 5), null, 1, 1, 0.9)),
                        "5678", List.of(new ParsedInvoiceItem("AMAZON", new BigDecimal("100.00"),
                                LocalDate.of(2024, 10, 7), null, 1, 1, 0.9))
                ));
        when(creditCardRepository.findById(1L)).thenReturn(Optional.of(card1234));
        when(creditCardRepository.findById(2L)).thenReturn(Optional.of(card5678));

        Invoice invoice1 = Invoice.of(card1234, YearMonth.of(2024, 11), LocalDate.of(2024, 11, 10));
        ReflectionTestUtils.setField(invoice1, "id", 100L);
        Invoice invoice2 = Invoice.of(card5678, YearMonth.of(2024, 11), LocalDate.of(2024, 11, 10));
        ReflectionTestUtils.setField(invoice2, "id", 101L);

        when(invoiceRepository.findByCreditCardAndMonth(card1234, YearMonth.of(2024, 11)))
                .thenReturn(List.of());
        when(invoiceRepository.findByCreditCardAndMonth(card5678, YearMonth.of(2024, 11)))
                .thenReturn(List.of());
        when(invoiceRepository.save(any(Invoice.class)))
                .thenReturn(invoice1)
                .thenReturn(invoice1)
                .thenReturn(invoice2)
                .thenReturn(invoice2);
        when(invoiceImportRepository.save(any(InvoiceImport.class))).thenReturn(importRecord);

        ConfirmImportRequest request = new ConfirmImportRequest(List.of(
                new ConfirmImportRequest.CardMapping("1234", 1L),
                new ConfirmImportRequest.CardMapping("5678", 2L)
        ));

        ConfirmImportResponse response = service.confirmImport(10L, request, testUser);

        assertThat(response.importId()).isEqualTo(10L);
        assertThat(response.createdInvoiceIds()).hasSize(2);
        assertThat(response.itemsImported()).isEqualTo(2);
        assertThat(response.message()).contains("successfully");
    }

    @Test
    void confirmImport_withWrongStatus_shouldThrowIllegalState() {
        InvoiceImport importRecord = InvoiceImport.of(testUser, ImportSource.PDF, "test.pdf", "/tmp/test.pdf");
        ReflectionTestUtils.setField(importRecord, "id", 10L);

        when(invoiceImportRepository.findByIdAndUser(10L, testUser))
                .thenReturn(Optional.of(importRecord));

        ConfirmImportRequest request = new ConfirmImportRequest(List.of(
                new ConfirmImportRequest.CardMapping("1234", 1L)
        ));

        assertThatThrownBy(() -> service.confirmImport(10L, request, testUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING_REVIEW");
    }
}
