package com.fintrack.application.invoice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.application.creditcard.MerchantCategorizationService;
import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.creditcard.InvoiceItem;
import com.fintrack.domain.invoice.InvoiceImport;
import com.fintrack.domain.invoice.ImportSource;
import com.fintrack.domain.invoice.ImportStatus;
import com.fintrack.domain.user.User;
import com.fintrack.dto.invoice.ConfirmImportRequest;
import com.fintrack.dto.invoice.ConfirmImportResponse;
import com.fintrack.dto.invoice.DetectedCardMapping;
import com.fintrack.dto.invoice.ImportInvoiceRequest;
import com.fintrack.dto.invoice.ImportInvoiceResponse;
import com.fintrack.dto.invoice.ImportPreviewResponse;
import com.fintrack.dto.invoice.ImportProgressResponse;
import com.fintrack.dto.invoice.ParsedInvoiceData;
import com.fintrack.infrastructure.persistence.creditcard.CreditCardJpaRepository;
import com.fintrack.infrastructure.persistence.creditcard.InvoiceJpaRepository;
import com.fintrack.infrastructure.persistence.invoice.InvoiceImportJpaRepository;
import com.fintrack.infrastructure.parsing.PdfInvoiceParser;
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
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.fintrack.domain.creditcard.InvoiceItem.of;

/**
 * Service for handling invoice imports from various sources.
 */
@Service
@Transactional
public class InvoiceImportService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceImportService.class);
    
    /** Delimiter used to separate fields when computing item signatures. */
    private static final String SIGNATURE_DELIMITER = "|";
    
    /** Default amount string representation for null values. */
    private static final String DEFAULT_AMOUNT = "0.00";
    
    /** Default number of installments when not specified. */
    private static final int DEFAULT_INSTALLMENTS = 1;
    
    /** Default number of days to add to the current date for due date calculation. */
    private static final int DEFAULT_DUE_DATE_DAYS = 30;
    
    /** Confidence threshold below which imports require manual review. */
    private static final double MANUAL_REVIEW_CONFIDENCE_THRESHOLD = 0.7;
    
    
    /** Compiled regex pattern for whitespace normalization. */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /** Pattern to strip date prefix like "02/02 " or "N 02/02 " from descriptions. */
    private static final Pattern DATE_PREFIX_PATTERN =
        Pattern.compile("^(?:\\d{1,2}\\s+)?\\d{2}/\\d{2}\\s+");

    /** Pattern to strip installment suffix like " 01/02" from descriptions. */
    private static final Pattern INSTALLMENT_SUFFIX_PATTERN =
        Pattern.compile("\\s+\\d{1,2}/\\d{1,2}$");

    /** The invoice import repository. */
    private final InvoiceImportJpaRepository invoiceImportRepository;
    /** The credit card repository. */
    private final CreditCardJpaRepository creditCardRepository;
    /** The invoice repository. */
    private final InvoiceJpaRepository invoiceRepository;
    /** The PDF invoice parser. */
    private final PdfInvoiceParser pdfInvoiceParser;
    /** The object mapper for JSON processing. */
    private final ObjectMapper objectMapper;
    /** The merchant categorization service. */
    private final MerchantCategorizationService merchantCategorizationService;
    /** The card auto-match service. */
    private final CardAutoMatchService cardAutoMatchService;

    /** The upload directory path. */
    @Value("${app.upload.directory:uploads}")
    private String uploadDirectory;

    public InvoiceImportService(InvoiceImportJpaRepository invoiceImportRepository,
                               CreditCardJpaRepository creditCardRepository,
                               InvoiceJpaRepository invoiceRepository,
                               PdfInvoiceParser pdfInvoiceParser,
                               ObjectMapper objectMapper,
                               MerchantCategorizationService merchantCategorizationService,
                               CardAutoMatchService cardAutoMatchService) {
        this.invoiceImportRepository = invoiceImportRepository;
        this.creditCardRepository = creditCardRepository;
        this.invoiceRepository = invoiceRepository;
        this.pdfInvoiceParser = pdfInvoiceParser;
        this.objectMapper = objectMapper;
        this.merchantCategorizationService = merchantCategorizationService;
        this.cardAutoMatchService = cardAutoMatchService;
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
    public ImportInvoiceResponse importInvoice(
            MultipartFile file, ImportInvoiceRequest request, User user) throws IOException {
        Validate.notNull(file, "File must not be null.");
        Validate.notNull(request, "Request must not be null.");
        // User validation is handled in the controller

        logger.info(
            "Starting invoice import for user: {}, file: {}", user.getEmail(), file.getOriginalFilename());

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
     * Uploads and parses a file, returning a preview with detected card sections
     * and auto-matched credit cards for user confirmation.
     *
     * @param file the uploaded file. Cannot be null.
     * @param user the user performing the import. Cannot be null.
     * @return the preview response with detected cards. Never null.
     * @throws IOException if there's an error processing the file.
     */
    public ImportPreviewResponse previewImport(MultipartFile file, User user) throws IOException {
        Validate.notNull(file, "File must not be null.");

        logger.info("Starting import preview for user: {}, file: {}",
                user.getEmail(), file.getOriginalFilename());

        ImportSource source = determineImportSource(file.getOriginalFilename());
        logger.info("Preview import: source={}, fileSize={}", source, file.getSize());

        String filePath = saveFileToDisk(file);
        logger.info("File saved to: {}", filePath);

        InvoiceImport importRecord = InvoiceImport.of(user, source, file.getOriginalFilename(), filePath);
        importRecord = invoiceImportRepository.save(importRecord);
        logger.info("Import record created: id={}", importRecord.getId());

        ParsedInvoiceData parsedData = parseFile(importRecord);
        logger.info("Parsed data: items={}, cardSections={}, bank={}, confidence={}",
                parsedData.items() != null ? parsedData.items().size() : 0,
                parsedData.cardSections() != null ? parsedData.cardSections().size() : 0,
                parsedData.bankName(), parsedData.confidence());

        updateImportWithParsedData(importRecord, parsedData);

        List<DetectedCardMapping> detectedCards = cardAutoMatchService.buildDetectedCardMappings(parsedData, user);
        boolean allMatched = detectedCards.stream().allMatch(DetectedCardMapping::autoMatched);
        logger.info("Card matching: detected={}, allMatched={}", detectedCards.size(), allMatched);

        importRecord.markAsPendingReview();
        invoiceImportRepository.save(importRecord);

        return new ImportPreviewResponse(
                importRecord.getId(),
                parsedData.bankName(),
                parsedData.invoiceMonth(),
                parsedData.dueDate(),
                parsedData.totalAmount(),
                parsedData.confidence(),
                detectedCards,
                allMatched
        );
    }

    /**
     * Confirms a previewed import by creating invoices for each mapped card.
     *
     * @param importId the import ID to confirm. Cannot be null.
     * @param request  the confirmation request with card mappings. Cannot be null.
     * @param user     the user confirming the import. Cannot be null.
     * @return the confirmation response with created invoice IDs. Never null.
     */
    public ConfirmImportResponse confirmImport(Long importId, ConfirmImportRequest request, User user) {
        Validate.notNull(importId, "Import ID must not be null.");
        Validate.notNull(request, "Request must not be null.");

        logger.info("Confirming import {}: {} card mappings", importId, request.cardMappings().size());

        InvoiceImport importRecord = invoiceImportRepository.findByIdAndUser(importId, user)
                .orElseThrow(() -> new IllegalArgumentException("Import not found or access denied."));

        if (importRecord.getStatus() != ImportStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Import is not in PENDING_REVIEW status. Current: " + importRecord.getStatus());
        }

        ParsedInvoiceData parsedData = deserializeParsedData(importRecord);

        Map<String, List<ParsedInvoiceData.ParsedInvoiceItem>> itemsByCard =
                cardAutoMatchService.buildItemsByCardMap(parsedData);
        logger.info("Items by card map: {} entries, keys={}", itemsByCard.size(), itemsByCard.keySet());

        LocalDate dueDate = parsedData.dueDate() != null
                ? parsedData.dueDate()
                : LocalDate.now().plusDays(DEFAULT_DUE_DATE_DAYS);
        YearMonth invoiceMonth = parsedData.invoiceMonth() != null
                ? parsedData.invoiceMonth()
                : YearMonth.from(dueDate);

        List<Long> createdInvoiceIds = new ArrayList<>();
        int totalItems = 0;

        String groupId = request.cardMappings().size() > 1
                ? UUID.randomUUID().toString() : null;

        // Build global signatures from ALL invoices of the same month to prevent
        // cross-card duplicates (e.g., old single-card import put all items in one invoice)
        Set<String> globalSignatures = buildGlobalSignatures(invoiceMonth);
        logger.info("Built {} global signatures for month {}", globalSignatures.size(), invoiceMonth);

        for (ConfirmImportRequest.CardMapping mapping : request.cardMappings()) {
            logger.info("Processing mapping: digits={}, creditCardId={}",
                    mapping.detectedLastFourDigits(), mapping.creditCardId());

            CreditCard creditCard = validateAndGetCreditCard(mapping.creditCardId(), user);
            List<ParsedInvoiceData.ParsedInvoiceItem> cardItems =
                    itemsByCard.getOrDefault(mapping.detectedLastFourDigits(), List.of());

            if (cardItems.isEmpty()) {
                logger.warn("No items found for card {} in import {}",
                        mapping.detectedLastFourDigits(), importId);
                continue;
            }

            Invoice invoice = findOrCreateInvoice(creditCard, invoiceMonth, dueDate);
            if (groupId != null) {
                invoice.setImportGroupId(groupId);
            }
            addItemsToInvoice(invoice, cardItems, globalSignatures);

            int categorized = merchantCategorizationService.applyRulesToItems(
                    invoice.getItems(), creditCard.getOwner());
            logger.info("Auto-categorized {} items for card {} in import {}",
                    categorized, mapping.detectedLastFourDigits(), importId);

            invoice = invoiceRepository.save(invoice);
            createdInvoiceIds.add(invoice.getId());
            totalItems += cardItems.size();
        }

        importRecord.markAsCompletedWithoutInvoiceReference();
        importRecord.setCreatedInvoiceIds(createdInvoiceIds.toString());
        invoiceImportRepository.save(importRecord);

        logger.info("Import {} confirmed: {} invoices created, {} items imported",
                importId, createdInvoiceIds.size(), totalItems);

        return new ConfirmImportResponse(
                "Import confirmed successfully.",
                importId,
                createdInvoiceIds,
                totalItems
        );
    }

    /**
     * Deserializes parsed data JSON from an import record.
     */
    private ParsedInvoiceData deserializeParsedData(InvoiceImport importRecord) {
        if (importRecord.getParsedData() == null) {
            throw new IllegalStateException("No parsed data for import: " + importRecord.getId());
        }
        try {
            return objectMapper.readValue(importRecord.getParsedData(), ParsedInvoiceData.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing parsed data for import: "
                    + importRecord.getId(), e);
        }
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

            // Force manual review when multiple cards are detected
            boolean multiCard = parsedData.cardSections() != null
                    && parsedData.cardSections().size() > 1;

            if (multiCard) {
                importRecord.markForManualReview();
                logger.info("Import {} requires review: {} card sections detected",
                    importId, parsedData.cardSections().size());
            } else if (requiresManualReview(parsedData.confidence())) {
                importRecord.markForManualReview();
                logger.info(
                    "Import {} marked for manual review due to low confidence: {}",
                    importId, parsedData.confidence());
            } else {
                // Create invoice automatically (single card only)
                logger.info("Creating invoice for import: {}", importId);
                Invoice invoice = createInvoiceFromParsedData(importRecord, parsedData);
                handleImportCompletion(importRecord, invoice);
                logger.info("Import {} completed successfully, invoice: {} with {} items",
                    importId, invoice.getId(), invoice.getItems().size());
            }

            invoiceImportRepository.save(importRecord);

        } catch (IOException e) {
            logger.error("Error parsing file for import: {}", importId, e);
            handleImportError(importId, "File parsing failed: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Runtime error processing import: {}", importId, e);
            handleImportError(importId, "Processing failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error processing import: {}", importId, e);
            handleImportError(importId, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Determines if an import requires manual review based on confidence score.
     *
     * @param confidence the parsing confidence score (0.0 to 1.0). Can be null.
     * @return true if manual review is required, false otherwise
     */
    private boolean requiresManualReview(Double confidence) {
        return confidence == null || confidence < MANUAL_REVIEW_CONFIDENCE_THRESHOLD;
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
        } else if (extension.endsWith(".jpg")
            || extension.endsWith(".jpeg")
            || extension.endsWith(".png")
            || extension.endsWith(".gif")) {
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
        String directory = uploadDirectory != null && !uploadDirectory.isBlank()
                ? uploadDirectory
                : System.getProperty("java.io.tmpdir");

        Path uploadPath = Paths.get(directory).toAbsolutePath();
        logger.info("Upload directory: {} (exists={}, writable={})",
                uploadPath, Files.exists(uploadPath),
                Files.exists(uploadPath) && Files.isWritable(uploadPath));

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            logger.info("Created upload directory: {}", uploadPath);
        }

        if (!Files.isWritable(uploadPath)) {
            // Fallback to temp directory if upload dir is not writable
            uploadPath = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath();
            logger.warn("Upload directory not writable, falling back to temp: {}", uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);
        logger.info("File saved: {} ({} bytes)", filePath, Files.size(filePath));

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
      // TODO: Implement image parsing with OCR
      // TODO: Implement email parsing
      return switch (importRecord.getSource()) {
        case PDF -> pdfInvoiceParser.parsePdf(importRecord.getFilePath());
        case IMAGE ->
                throw new UnsupportedOperationException("Image parsing not yet implemented.");
        case EMAIL ->
                throw new UnsupportedOperationException("Email parsing not yet implemented.");
        default ->
                throw new UnsupportedOperationException("Manual imports should not be processed automatically.");
      };
    }

    /**
     * Updates an import record with parsed data.
     *
     * @param importRecord the import record. Cannot be null.
     * @param parsedData the parsed data. Cannot be null.
     */
    private void updateImportWithParsedData(
            InvoiceImport importRecord, ParsedInvoiceData parsedData) {
        try {
            importRecord.setParsedData(objectMapper.writeValueAsString(parsedData));
            importRecord.setTotalAmount(parsedData.totalAmount());
            importRecord.setDueDate(parsedData.dueDate() != null
                ? parsedData.dueDate().atStartOfDay()
                : null);
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
    private Invoice createInvoiceFromParsedData(
            InvoiceImport importRecord, ParsedInvoiceData parsedData) {
        logger.info("Creating invoice from parsed data for import: {}", importRecord.getId());
        
        CreditCard creditCard = validateCreditCard(importRecord);
        LocalDate dueDate = resolveDueDate(parsedData);
        YearMonth invoiceMonth = parsedData.invoiceMonth() != null
            ? parsedData.invoiceMonth()
            : YearMonth.from(dueDate);
        Invoice invoice = findOrCreateInvoice(creditCard, invoiceMonth, dueDate);
        
        addItemsToInvoice(invoice, parsedData.items(), Set.of());
        
        // Apply merchant categorization rules to new items
        User owner = creditCard.getOwner();
        int categorized = merchantCategorizationService.applyRulesToItems(invoice.getItems(), owner);
        logger.info("Auto-categorized {} items for import {}", categorized, importRecord.getId());
        
        return invoiceRepository.save(invoice);
    }

    /**
     * Validates that the import record has a valid credit card associated.
     *
     * @param importRecord the import record to validate. Cannot be null.
     * @return the validated credit card. Never null.
     * @throws IllegalStateException if no credit card is found for the import
     */
    private CreditCard validateCreditCard(InvoiceImport importRecord) {
        CreditCard creditCard = importRecord.getCreditCard();
        if (creditCard == null) {
            throw new IllegalStateException("Credit card not found for import: " + importRecord.getId());
        }
        return creditCard;
    }

    /**
     * Resolves the due date from parsed data, using default if not provided.
     *
     * @param parsedData the parsed invoice data. Cannot be null.
     * @return the resolved due date. Never null.
     */
    private LocalDate resolveDueDate(ParsedInvoiceData parsedData) {
        return parsedData.dueDate() != null 
            ? parsedData.dueDate() 
            : LocalDate.now().plusDays(DEFAULT_DUE_DATE_DAYS);
    }

    /**
     * Finds an existing invoice for the given credit card and month, or creates a new one.
     *
     * @param creditCard the credit card to find/create invoice for. Cannot be null.
     * @param month the month to determine the invoice. Cannot be null.
     * @param dueDate the due date to determine the invoice month. Cannot be null.
     * @return the existing or newly created invoice. Never null.
     */
    private Invoice findOrCreateInvoice(CreditCard creditCard, YearMonth month, LocalDate dueDate) {
        
        return invoiceRepository.findByCreditCardAndMonth(creditCard, month)
            .stream()
            .findFirst()
            .map(invoice -> {
                logger.info("Found existing invoice {} for card {} and month {}", 
                    invoice.getId(), creditCard.getId(), month);
                return invoice;
            })
            .orElseGet(() -> {
                Invoice newInvoice = Invoice.of(creditCard, month, dueDate);
                Invoice saved = invoiceRepository.save(newInvoice);
                logger.info("Created new invoice {} for card {} and month {}", 
                    saved.getId(), creditCard.getId(), month);
                return saved;
            });
    }

    /**
     * Adds parsed items to an invoice with duplicate detection and removal.
     * Items are deduplicated based on their computed signature to prevent
     * adding the same item multiple times during re-imports.
     *
     * @param invoice the invoice to add items to. Cannot be null.
     * @param parsedItems the list of parsed items to add. Can be null or empty.
     * @param extraSignatures additional signatures (e.g., from other invoices of the same month)
     *                        to check for cross-invoice duplicates. Can be empty.
     */
    private void addItemsToInvoice(
            Invoice invoice, List<ParsedInvoiceData.ParsedInvoiceItem> parsedItems,
            Set<String> extraSignatures) {
        if (parsedItems == null || parsedItems.isEmpty()) {
            logger.warn("No items found in parsed data for invoice: {}", invoice.getId());
            return;
        }

        long startTime = System.currentTimeMillis();
        int initialItemCount = invoice.getItems().size();

        logger.info("Adding {} items to invoice {} (with occurrence-aware deduplication)",
                parsedItems.size(), invoice.getId());

        Map<String, Integer> existingCounts = buildExistingSignatureCounts(invoice);
        for (String sig : extraSignatures) {
            if (!existingCounts.containsKey(sig)) {
                existingCounts.put(sig, 1);
            }
        }
        ItemAdditionResult result = processItems(invoice, parsedItems, existingCounts);

        long processingTime = System.currentTimeMillis() - startTime;
        int finalItemCount = invoice.getItems().size();
        
        logger.info("Invoice {} updated in {}ms. Items added: {}, skipped: {}, total: {} -> {}",
            invoice.getId(), processingTime, result.added(), result.skipped(), 
            initialItemCount, finalItemCount);
        
        if (result.totalProcessed() > 0) {
            logger.debug("Import performance - Success rate: {}%, Processing time: {}ms per item", 
                String.format("%.1f", result.successRate()),
                processingTime / result.totalProcessed());
        }
    }

    /**
     * Builds signature counts from ALL invoices of the given month.
     * Used during multi-card confirm to detect items already imported
     * by a previous single-card import into the wrong invoice.
     */
    private Set<String> buildGlobalSignatures(YearMonth month) {
        List<Invoice> allMonthInvoices = invoiceRepository.findByMonth(month);
        Set<String> signatures = new HashSet<>();
        for (Invoice inv : allMonthInvoices) {
            inv.getItems().stream()
                .map(this::safeComputeSignature)
                .filter(sig -> !sig.isEmpty())
                .forEach(signatures::add);
        }
        return signatures;
    }

    /**
     * Builds a count map of signatures for all existing items in the invoice.
     * Counts occurrences so that legitimate duplicate items (same description,
     * amount, date) are tracked individually.
     */
    private Map<String, Integer> buildExistingSignatureCounts(Invoice invoice) {
        List<InvoiceItem> items = invoice.getItems();
        if (items.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Integer> counts = new HashMap<>(items.size());
        for (InvoiceItem item : items) {
            String sig = safeComputeSignature(item);
            if (!sig.isEmpty()) {
                counts.merge(sig, 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * Safely computes a signature for an existing invoice item.
     * REFACTORED: Enhanced error handling and logging.
     * Returns empty string if signature computation fails to avoid breaking the import process.
     *
     * @param item the invoice item to compute signature for. Cannot be null.
     * @return the computed signature or empty string if computation fails. Never null.
     */
    private String safeComputeSignature(InvoiceItem item) {
        try {
            return computeItemSignature(
                item.getDescription(),
                item.getAmount(),
                item.getPurchaseDate(),
                item.getInstallments(),
                item.getTotalInstallments()
            );
        } catch (Exception ex) {
            logger.debug("Failed to compute signature for existing item {}: {}", 
                item.getId(), ex.getMessage());
            return "";
        }
    }

    /**
     * Processes parsed items and adds non-duplicate ones to the invoice.
     * Each item is checked against existing signatures to prevent duplicates.
     *
     * @param invoice the invoice to add items to. Cannot be null.
     * @param parsedItems the list of parsed items to process. Cannot be null or empty.
     * @param signatureCounts count map of existing item signatures for occurrence-aware duplicate detection.
     * @return the result containing counts of added and skipped items. Never null.
     */
    private ItemAdditionResult processItems(Invoice invoice, 
                                          List<ParsedInvoiceData.ParsedInvoiceItem> parsedItems,
                                          Map<String, Integer> signatureCounts) {
        int added = 0;
        int skipped = 0;

        Map<String, Integer> batchConsumed = new HashMap<>();

        for (ParsedInvoiceData.ParsedInvoiceItem parsedItem : parsedItems) {
            try {
                String signature = computeItemSignature(parsedItem);
                int alreadyExists = signatureCounts.getOrDefault(signature, 0);
                int consumed = batchConsumed.getOrDefault(signature, 0);

                if (consumed < alreadyExists) {
                    batchConsumed.merge(signature, 1, Integer::sum);
                    skipped++;
                    logger.debug("Skipped duplicate item by signature: {}", parsedItem.description());
                    continue;
                }

                InvoiceItem invoiceItem = createInvoiceItem(invoice, parsedItem);
                invoice.addItem(invoiceItem);
                signatureCounts.merge(signature, 1, Integer::sum);
                batchConsumed.merge(signature, 1, Integer::sum);
                added++;
                logger.debug("Added new item: {} (amount: {}, date: {})",
                    parsedItem.description(), parsedItem.amount(), parsedItem.purchaseDate());

            } catch (RuntimeException e) {
                logger.warn("Error adding item to invoice: {} - {}", 
                    parsedItem.description(), e.getMessage());
                skipped++;
            } catch (Exception e) {
                logger.warn("Unexpected error adding item to invoice: {}", parsedItem.description(), e);
                skipped++;
            }
        }

        return new ItemAdditionResult(added, skipped);
    }

    /**
     * Creates an InvoiceItem from parsed invoice item data.
     * Resolves null values to appropriate defaults for dates and installments.
     *
     * @param invoice the invoice this item belongs to. Cannot be null.
     * @param parsedItem the parsed item data. Cannot be null.
     * @return the created invoice item. Never null.
     */
    private InvoiceItem createInvoiceItem(Invoice invoice, ParsedInvoiceData.ParsedInvoiceItem parsedItem) {
        return of(
            invoice,
            parsedItem.description(),
            parsedItem.amount(),
            null,
            resolveDate(parsedItem.purchaseDate()),
            resolveInstallments(parsedItem.installments()),
            resolveInstallments(parsedItem.totalInstallments())
        );
    }

    /**
     * Resolves a date value, using current date as default if null.
     *
     * @param date the date to resolve. Can be null.
     * @return the resolved date. Never null.
     */
    private LocalDate resolveDate(LocalDate date) {
        return date != null ? date : LocalDate.now();
    }

    /**
     * Resolves an installments value, using default if null.
     *
     * @param installments the installments value to resolve. Can be null.
     * @return the resolved installments value. Never null, always positive.
     */
    private int resolveInstallments(Integer installments) {
        return installments != null ? installments : DEFAULT_INSTALLMENTS;
    }

    /**
     * Result of processing items during import, containing counts of added and skipped items.
     * 
     * @param added number of items successfully added
     * @param skipped number of items skipped (duplicates or invalid)
     */
    public record ItemAdditionResult(int added, int skipped) {
        public ItemAdditionResult {
            if (added < 0) {
                throw new IllegalArgumentException("Added count cannot be negative");
            }
            if (skipped < 0) {
                throw new IllegalArgumentException("Skipped count cannot be negative");
            }
        }

        /**
         * Total number of items processed (added + skipped).
         */
        public int totalProcessed() {
            return added + skipped;
        }

        /**
         * Success rate as a percentage (0-100).
         */
        public double successRate() {
            if (totalProcessed() == 0) {
                return 0.0;
            }
            return (double) added / totalProcessed() * 100.0;
        }
    }

    /**
     * Computes a stable signature for an invoice item based on normalized fields.
     * REFACTORED: Optimized signature computation with better performance and cleaner code.
     */
    private String computeItemSignature(String description,
                                        BigDecimal amount,
                                        LocalDate purchaseDate,
                                        int installment,
                                        int totalInstallments) {
        // Pre-compute normalized values to avoid repeated calculations
        String normalizedDescription = normalizeDescription(description);
        String normalizedAmount = normalizeAmount(amount);
        String normalizedDate = purchaseDate != null ? purchaseDate.toString() : "";

        // Build signature based on item type for optimal deduplication
        String base = buildSignatureBase(normalizedDescription, normalizedAmount, 
                                       normalizedDate, installment, totalInstallments);
        
        return sha256(base);
    }

    /**
     * Builds the signature base string based on item type for optimal deduplication.
     * REFACTORED: Extracted method for better readability and maintainability.
     *
     * @param normalizedDescription the normalized description. Cannot be null.
     * @param normalizedAmount the normalized amount. Cannot be null.
     * @param normalizedDate the normalized date. Cannot be null.
     * @param installment the installment number.
     * @param totalInstallments the total installments.
     * @return the signature base string. Never null.
     */
    private String buildSignatureBase(String normalizedDescription, String normalizedAmount,
                                    String normalizedDate, int installment, int totalInstallments) {
        return String.join(SIGNATURE_DELIMITER,
                normalizedDescription,
                normalizedAmount,
                normalizedDate,
                String.valueOf(installment),
                String.valueOf(totalInstallments)
        );
    }

    /**
     * Computes a signature for a parsed invoice item using resolved default values.
     * REFACTORED: Optimized method with better error handling.
     *
     * @param parsedItem the parsed invoice item. Cannot be null.
     * @return the computed signature. Never null or empty.
     */
    private String computeItemSignature(ParsedInvoiceData.ParsedInvoiceItem parsedItem) {
        return computeItemSignature(
            parsedItem.description(),
            parsedItem.amount(),
            resolveDate(parsedItem.purchaseDate()),
            resolveInstallments(parsedItem.installments()),
            resolveInstallments(parsedItem.totalInstallments())
        );
    }

    /**
     * Normalizes a BigDecimal amount to a consistent string representation.
     * REFACTORED: Optimized with better null handling and performance.
     *
     * @param amount the amount to normalize. Can be null.
     * @return the normalized amount string. Never null.
     */
    private String normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return DEFAULT_AMOUNT;
        }
        
        // Use setScale with RoundingMode.HALF_UP for consistent decimal places
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Normalizes a description string for consistent signature generation.
     * REFACTORED: Optimized with better performance and cleaner regex.
     *
     * @param description the description to normalize. Can be null.
     * @return the normalized description. Never null.
     */
    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }

        String trimmed = description.trim().toLowerCase();
        // Collapse multiple whitespaces into single space
        trimmed = WHITESPACE_PATTERN.matcher(trimmed).replaceAll(" ");
        // Strip date prefix (e.g., "02/02 " or "1 02/02 ") — already captured in date field
        trimmed = DATE_PREFIX_PATTERN.matcher(trimmed).replaceAll("");
        // Strip installment suffix (e.g., " 01/02") — already captured in installment fields
        trimmed = INSTALLMENT_SUFFIX_PATTERN.matcher(trimmed).replaceAll("");
        return trimmed;
    }

    /**
     * Computes SHA-256 hash of the input string for signature generation.
     * REFACTORED: Optimized with better performance and error handling.
     *
     * @param input the string to hash. Cannot be null.
     * @return the SHA-256 hash as hex string, or raw input as fallback. Never null.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hex = new StringBuilder(hash.length * 2);
            
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                // Ensure two-digit hex representation
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to raw input if SHA-256 is unavailable (highly unlikely)
            logger.warn("SHA-256 not available, using raw signature input: {}", e.getMessage());
            return input;
        }
    }

    /**
     * Handles the completion of an import by checking for existing imports that reference
     * the same invoice and reusing them to avoid unique constraint violations.
     *
     * @param importRecord the current import record. Cannot be null.
     * @param invoice the invoice that was created/updated. Cannot be null.
     */
    private void handleImportCompletion(InvoiceImport importRecord, Invoice invoice) {
        List<InvoiceImport> existingImports =
                invoiceImportRepository.findByCreatedInvoiceId(invoice.getId());
        
        if (existingImports.isEmpty()) {
            // No existing import references this invoice, safe to mark as completed
            importRecord.markAsCompleted(invoice);
            logger.info("Import {} marked as completed with invoice {}",
                    importRecord.getId(), invoice.getId());
        } else {
            // Invoice already referenced by another import, mark as completed without reference
            importRecord.markAsCompletedWithoutInvoiceReference();
            logger.info("Import {} completed successfully. Invoice {} already referenced by import {}", 
                importRecord.getId(), invoice.getId(), existingImports.get(0).getId());
        }
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
        return status.getDisplayName();
    }
}

