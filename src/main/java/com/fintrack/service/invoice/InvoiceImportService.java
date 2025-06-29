package com.fintrack.service.invoice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.invoice.InvoiceImport;
import com.fintrack.domain.invoice.ImportSource;
import com.fintrack.domain.invoice.ImportStatus;
import com.fintrack.domain.user.User;
import com.fintrack.dto.invoice.ImportInvoiceRequest;
import com.fintrack.dto.invoice.ImportInvoiceResponse;
import com.fintrack.dto.invoice.ImportProgressResponse;
import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceJpaRepository;
import com.fintrack.infrastructure.persistence.invoice.InvoiceImportJpaRepository;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling invoice imports from various sources.
 */
@Service
@Transactional
public class InvoiceImportService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceImportService.class);

    private final InvoiceImportJpaRepository invoiceImportRepository;
    private final CreditCardJpaRepository creditCardRepository;
    private final InvoiceJpaRepository invoiceRepository;
    private final PdfInvoiceParser pdfInvoiceParser;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.directory:uploads}")
    private String uploadDirectory;

    public InvoiceImportService(InvoiceImportJpaRepository invoiceImportRepository,
                               CreditCardJpaRepository creditCardRepository,
                               InvoiceJpaRepository invoiceRepository,
                               PdfInvoiceParser pdfInvoiceParser,
                               ObjectMapper objectMapper) {
        this.invoiceImportRepository = invoiceImportRepository;
        this.creditCardRepository = creditCardRepository;
        this.invoiceRepository = invoiceRepository;
        this.pdfInvoiceParser = pdfInvoiceParser;
        this.objectMapper = objectMapper;
    }

    /**
     * Initiates an invoice import from an uploaded file.
     *
     * @param file the uploaded file. Cannot be null.
     * @param request the import request. Cannot be null.
     * @param user the user performing the import. Cannot be null.
     * @return the import response. Never null.
     * @throws IOException if there's an error processing the file.
     */
    public ImportInvoiceResponse importInvoice(MultipartFile file, ImportInvoiceRequest request, User user) throws IOException {
        Validate.notNull(file, "File must not be null.");
        Validate.notNull(request, "Request must not be null.");
        // User validation is handled in the controller

        logger.info("Starting invoice import for user: {}, file: {}", user.getEmail(), file.getOriginalFilename());

        // Validate and get credit card first
        CreditCard creditCard = validateAndGetCreditCard(request.creditCardId(), user);

        // Determine import source
        ImportSource source = determineImportSource(file.getOriginalFilename());

        // Save file to disk
        String filePath = saveFileToDisk(file);

        // Create import record
        InvoiceImport importRecord = InvoiceImport.of(user, source, file.getOriginalFilename(), filePath);
        importRecord.setCreditCard(creditCard);
        importRecord = invoiceImportRepository.save(importRecord);

        // Start async processing
        processImportAsync(importRecord.getId());

        return createImportResponse(importRecord, "Import iniciado com sucesso. Processando em background.");
    }

    /**
     * Gets the progress of an import.
     *
     * @param importId the import ID. Cannot be null.
     * @param user the user requesting the progress. Cannot be null.
     * @return the import progress response. Never null.
     */
    @Transactional(readOnly = true)
    public ImportProgressResponse getImportProgress(Long importId, User user) {
        Validate.notNull(importId, "Import ID must not be null.");
        // User validation is handled in the controller

        InvoiceImport importRecord = invoiceImportRepository.findByIdAndUser(importId, user)
            .orElseThrow(() -> new IllegalArgumentException("Import not found or access denied."));

        return createProgressResponse(importRecord);
    }

    /**
     * Gets all imports for a user.
     *
     * @param user the user. Cannot be null.
     * @return a list of import responses. Never null, may be empty.
     */
    @Transactional(readOnly = true)
    public List<ImportInvoiceResponse> getUserImports(User user) {
        // User validation is handled in the controller

        List<InvoiceImport> imports = invoiceImportRepository.findByUserOrderByImportedAtDesc(user);
        return imports.stream()
            .map(this::createImportResponse)
            .toList();
    }

    /**
     * Gets imports by status for a user.
     *
     * @param user the user. Cannot be null.
     * @param status the status to filter by. Cannot be null.
     * @return a list of import responses. Never null, may be empty.
     */
    @Transactional(readOnly = true)
    public List<ImportInvoiceResponse> getUserImportsByStatus(User user, ImportStatus status) {
        // User validation is handled in the controller
        Validate.notNull(status, "Status must not be null.");

        List<InvoiceImport> imports = invoiceImportRepository.findByUserAndStatusOrderByImportedAtDesc(user, status);
        return imports.stream()
            .map(this::createImportResponse)
            .toList();
    }

    /**
     * Processes an import asynchronously.
     *
     * @param importId the import ID to process.
     */
    @Async
    public void processImportAsync(Long importId) {
        try {
            InvoiceImport importRecord = invoiceImportRepository.findById(importId)
                .orElseThrow(() -> new IllegalArgumentException("Import not found: " + importId));

            logger.info("Processing import: {}", importId);
            importRecord.markAsProcessing();
            invoiceImportRepository.save(importRecord);

            // Parse the file based on source
            logger.info("Parsing file for import: {}", importId);
            ParsedInvoiceData parsedData = parseFile(importRecord);
            logger.info("Parsed data for import {}: {} items, total: {}, dueDate: {}", 
                importId, parsedData.items() != null ? parsedData.items().size() : 0, 
                parsedData.totalAmount(), parsedData.dueDate());

            // Update import record with parsed data
            updateImportWithParsedData(importRecord, parsedData);

            // Determine if manual review is needed
            if (parsedData.confidence() < 0.7) {
                importRecord.markForManualReview();
                logger.info("Import {} marked for manual review due to low confidence: {}", importId, parsedData.confidence());
            } else {
                // Create invoice automatically
                logger.info("Creating invoice for import: {}", importId);
                Invoice invoice = createInvoiceFromParsedData(importRecord, parsedData);
                importRecord.markAsCompleted(invoice);
                logger.info("Import {} completed successfully, created invoice: {} with {} items", 
                    importId, invoice.getId(), invoice.getItems().size());
            }

            invoiceImportRepository.save(importRecord);

        } catch (Exception e) {
            logger.error("Error processing import: {}", importId, e);
            handleImportError(importId, e.getMessage());
        }
    }

    /**
     * Validates and retrieves a credit card.
     *
     * @param creditCardId the credit card ID. Cannot be null.
     * @param user the user. Cannot be null.
     * @return the credit card. Never null.
     */
    private CreditCard validateAndGetCreditCard(Long creditCardId, User user) {
        CreditCard creditCard = creditCardRepository.findById(creditCardId)
            .orElseThrow(() -> new IllegalArgumentException("Credit card not found."));

        if (!creditCard.getOwner().equals(user)) {
            throw new IllegalArgumentException("Credit card does not belong to user.");
        }

        return creditCard;
    }

    /**
     * Determines the import source based on file extension.
     *
     * @param fileName the file name. Cannot be null.
     * @return the import source. Never null.
     */
    private ImportSource determineImportSource(String fileName) {
        if (fileName == null) {
            return ImportSource.MANUAL;
        }

        String extension = fileName.toLowerCase();
        if (extension.endsWith(".pdf")) {
            return ImportSource.PDF;
        } else if (extension.endsWith(".jpg") || extension.endsWith(".jpeg") || 
                   extension.endsWith(".png") || extension.endsWith(".gif")) {
            return ImportSource.IMAGE;
        } else {
            return ImportSource.MANUAL;
        }
    }

    /**
     * Saves a file to disk.
     *
     * @param file the file to save. Cannot be null.
     * @return the file path. Never null.
     * @throws IOException if there's an error saving the file.
     */
    private String saveFileToDisk(MultipartFile file) throws IOException {
        // Use upload directory or fallback to temp directory for tests
        String directory = uploadDirectory != null ? uploadDirectory : System.getProperty("java.io.tmpdir");
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(directory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        return filePath.toString();
    }

    /**
     * Parses a file based on its source type.
     *
     * @param importRecord the import record. Cannot be null.
     * @return the parsed data. Never null.
     * @throws IOException if there's an error parsing the file.
     */
    private ParsedInvoiceData parseFile(InvoiceImport importRecord) throws IOException {
        switch (importRecord.getSource()) {
            case PDF:
                return pdfInvoiceParser.parsePdf(importRecord.getFilePath());
            case IMAGE:
                // TODO: Implement image parsing with OCR
                throw new UnsupportedOperationException("Image parsing not yet implemented.");
            case EMAIL:
                // TODO: Implement email parsing
                throw new UnsupportedOperationException("Email parsing not yet implemented.");
            case MANUAL:
            default:
                throw new UnsupportedOperationException("Manual imports should not be processed automatically.");
        }
    }

    /**
     * Updates an import record with parsed data.
     *
     * @param importRecord the import record. Cannot be null.
     * @param parsedData the parsed data. Cannot be null.
     */
    private void updateImportWithParsedData(InvoiceImport importRecord, ParsedInvoiceData parsedData) {
        try {
            importRecord.setParsedData(objectMapper.writeValueAsString(parsedData));
            importRecord.setTotalAmount(parsedData.totalAmount());
            importRecord.setDueDate(parsedData.dueDate() != null ? 
                parsedData.dueDate().atStartOfDay() : null);
            importRecord.setBankName(parsedData.bankName());
            importRecord.setCardLastFourDigits(parsedData.cardNumber());
        } catch (JsonProcessingException e) {
            logger.error("Error serializing parsed data", e);
            throw new RuntimeException("Error processing parsed data", e);
        }
    }

    /**
     * Creates an invoice from parsed data.
     *
     * @param importRecord the import record. Cannot be null.
     * @param parsedData the parsed data. Cannot be null.
     * @return the created invoice. Never null.
     */
    private Invoice createInvoiceFromParsedData(InvoiceImport importRecord, ParsedInvoiceData parsedData) {
        logger.info("Creating invoice from parsed data for import: {}", importRecord.getId());
        
        CreditCard creditCard = importRecord.getCreditCard();
        if (creditCard == null) {
            throw new IllegalStateException("Credit card not found for import: " + importRecord.getId());
        }
        
        LocalDate dueDate = parsedData.dueDate() != null ? parsedData.dueDate() : LocalDate.now().plusDays(30);
        YearMonth month = YearMonth.from(dueDate);
        
        // Buscar se já existe fatura para o cartão e mês
        List<Invoice> existingInvoices = invoiceRepository.findByCreditCardAndMonth(creditCard, month);
        Invoice invoice;
        if (!existingInvoices.isEmpty()) {
            invoice = existingInvoices.get(0);
            logger.info("Found existing invoice {} for card {} and month {}", invoice.getId(), creditCard.getId(), month);
        } else {
            invoice = Invoice.of(
                creditCard,
                month,
                dueDate
            );
            invoice = invoiceRepository.save(invoice);
            logger.info("Created new invoice {} for card {} and month {}", invoice.getId(), creditCard.getId(), month);
        }
        
        // Adiciona os itens extraídos do PDF
        if (parsedData.items() != null && !parsedData.items().isEmpty()) {
            logger.info("Adding {} items to invoice {}", parsedData.items().size(), invoice.getId());
            
            for (ParsedInvoiceData.ParsedInvoiceItem parsedItem : parsedData.items()) {
                try {
                    com.fintrack.domain.creditcard.InvoiceItem invoiceItem = com.fintrack.domain.creditcard.InvoiceItem.of(
                        invoice,
                        parsedItem.description(),
                        parsedItem.amount(),
                        null, // category - será null por enquanto
                        parsedItem.purchaseDate() != null ? parsedItem.purchaseDate() : LocalDate.now(),
                        parsedItem.installments() != null ? parsedItem.installments() : 1,
                        parsedItem.totalInstallments() != null ? parsedItem.totalInstallments() : 1
                    );
                    invoice.addItem(invoiceItem);
                    logger.debug("Added item: {} - R$ {}", parsedItem.description(), parsedItem.amount());
                } catch (Exception e) {
                    logger.warn("Error adding item to invoice: {}", parsedItem.description(), e);
                }
            }
            invoice = invoiceRepository.save(invoice);
            logger.info("Invoice {} created/updated with {} items", invoice.getId(), invoice.getItems().size());
        } else {
            logger.warn("No items found in parsed data for import: {}", importRecord.getId());
        }
        
        return invoice;
    }

    /**
     * Handles import errors.
     *
     * @param importId the import ID. Cannot be null.
     * @param errorMessage the error message. Cannot be null.
     */
    private void handleImportError(Long importId, String errorMessage) {
        try {
            InvoiceImport importRecord = invoiceImportRepository.findById(importId)
                .orElseThrow(() -> new IllegalArgumentException("Import not found: " + importId));

            importRecord.markAsFailed(errorMessage);
            invoiceImportRepository.save(importRecord);
        } catch (Exception e) {
            logger.error("Error handling import failure: {}", importId, e);
        }
    }

    /**
     * Creates an import response from an import record.
     *
     * @param importRecord the import record. Cannot be null.
     * @return the import response. Never null.
     */
    private ImportInvoiceResponse createImportResponse(InvoiceImport importRecord) {
        return createImportResponse(importRecord, null);
    }

    /**
     * Creates an import response from an import record with a custom message.
     *
     * @param importRecord the import record. Cannot be null.
     * @param message the custom message. Can be null.
     * @return the import response. Never null.
     */
    private ImportInvoiceResponse createImportResponse(InvoiceImport importRecord, String message) {
        return new ImportInvoiceResponse(
            message != null ? message : "Import processed successfully.",
            importRecord.getId(),
            importRecord.getStatus(),
            importRecord.getSource(),
            importRecord.getOriginalFileName(),
            importRecord.getErrorMessage(),
            importRecord.getImportedAt(),
            importRecord.getProcessedAt(),
            importRecord.getTotalAmount(),
            importRecord.getBankName(),
            importRecord.getCardLastFourDigits()
        );
    }

    /**
     * Creates a progress response from an import record.
     *
     * @param importRecord the import record. Cannot be null.
     * @return the progress response. Never null.
     */
    private ImportProgressResponse createProgressResponse(InvoiceImport importRecord) {
        ParsedInvoiceData parsedData = null;
        if (importRecord.getParsedData() != null) {
            try {
                parsedData = objectMapper.readValue(importRecord.getParsedData(), ParsedInvoiceData.class);
            } catch (JsonProcessingException e) {
                logger.warn("Error deserializing parsed data for import: {}", importRecord.getId(), e);
            }
        }

        return new ImportProgressResponse(
            importRecord.getId(),
            importRecord.getStatus(),
            getStatusMessage(importRecord.getStatus()),
            importRecord.getImportedAt(),
            importRecord.getProcessedAt(),
            importRecord.getErrorMessage(),
            parsedData,
            importRecord.getTotalAmount(),
            importRecord.getBankName(),
            importRecord.getCardLastFourDigits(),
            importRecord.getStatus() == ImportStatus.MANUAL_REVIEW
        );
    }

    /**
     * Gets a status message for a given status.
     *
     * @param status the status. Cannot be null.
     * @return the status message. Never null.
     */
    private String getStatusMessage(ImportStatus status) {
        return switch (status) {
            case PENDING -> "Aguardando processamento";
            case PROCESSING -> "Processando arquivo";
            case COMPLETED -> "Importação concluída com sucesso";
            case FAILED -> "Falha na importação";
            case MANUAL_REVIEW -> "Requer revisão manual";
        };
    }
} 