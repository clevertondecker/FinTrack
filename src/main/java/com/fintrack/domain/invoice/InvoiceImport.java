package com.fintrack.domain.invoice;

import com.fintrack.domain.creditcard.CreditCard;
import com.fintrack.domain.creditcard.Invoice;
import com.fintrack.domain.user.User;
import jakarta.persistence.*;
import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an invoice import process.
 * Tracks the import of invoices from various sources (PDF, images, etc.).
 */
@Entity
@Table(name = "invoice_imports")
public class InvoiceImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status = ImportStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportSource source;

    @Column(length = 500, nullable = false)
    private String originalFileName;

    @Column(length = 1000, nullable = false)
    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String parsedData; // JSON with extracted data

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime importedAt;

    @Column
    private LocalDateTime processedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_invoice_id")
    private Invoice createdInvoice;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column
    private LocalDateTime dueDate;

    @Column(length = 100)
    private String bankName;

    @Column(length = 20)
    private String cardLastFourDigits;

    /**
     * Protected constructor for JPA only.
     */
    protected InvoiceImport() {}

    /**
     * Private constructor for InvoiceImport. Use the static factory method to create instances.
     */
    private InvoiceImport(final User theUser, final ImportSource theSource,
                         final String theOriginalFileName, final String theFilePath) {
        Validate.notNull(theUser, "User must not be null.");
        Validate.notBlank(theOriginalFileName, "Original file name must not be blank.");
        Validate.notBlank(theFilePath, "File path must not be blank.");
        Validate.notNull(theSource, "Import source must not be null.");

        user = theUser;
        source = theSource;
        originalFileName = theOriginalFileName;
        filePath = theFilePath;
        importedAt = LocalDateTime.now();
    }

    /**
     * Static factory method to create a new InvoiceImport instance.
     *
     * @param user the user importing the invoice. Cannot be null.
     * @param source the source type of the import. Cannot be null.
     * @param originalFileName the original file name. Cannot be null or blank.
     * @param filePath the path where the file is stored. Cannot be null or blank.
     * @return a validated InvoiceImport entity. Never null.
     */
    public static InvoiceImport of(final User user, final ImportSource source,
                                  final String originalFileName, final String filePath) {
        return new InvoiceImport(user, source, originalFileName, filePath);
    }

    /**
     * Marks the import as processing.
     */
    public void markAsProcessing() {
        status = ImportStatus.PROCESSING;
    }

    /**
     * Marks the import as completed with the created invoice.
     *
     * @param invoice the invoice that was created from this import.
     */
    public void markAsCompleted(final Invoice invoice) {
        Validate.notNull(invoice, "Invoice must not be null.");
        status = ImportStatus.COMPLETED;
        createdInvoice = invoice;
        processedAt = LocalDateTime.now();
    }

    /**
     * Marks the import as failed with an error message.
     *
     * @param errorMessage the error message describing why the import failed.
     */
    public void markAsFailed(final String errorMessage) {
        Validate.notBlank(errorMessage, "Error message must not be blank.");
        status = ImportStatus.FAILED;
        this.errorMessage = errorMessage;
        processedAt = LocalDateTime.now();
    }

    /**
     * Marks the import for manual review.
     */
    public void markForManualReview() {
        status = ImportStatus.MANUAL_REVIEW;
        processedAt = LocalDateTime.now();
    }

    /**
     * Marks the import as completed without referencing a specific invoice.
     * Used when the invoice is already referenced by another import to avoid
     * unique constraint violations.
     */
    public void markAsCompletedWithoutInvoiceReference() {
        status = ImportStatus.COMPLETED;
        processedAt = LocalDateTime.now();
        // Do not set createdInvoice to avoid unique constraint violation
    }

    /**
     * Sets the parsed data as JSON.
     *
     * @param parsedData the parsed data in JSON format. Can be null.
     */
    public void setParsedData(final String parsedData) {
        this.parsedData = parsedData;
    }

    /**
     * Sets the credit card for this import.
     *
     * @param creditCard the credit card. Can be null.
     */
    public void setCreditCard(final CreditCard creditCard) {
        this.creditCard = creditCard;
    }

    /**
     * Sets the total amount extracted from the invoice.
     *
     * @param totalAmount the total amount. Can be null.
     */
    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * Sets the due date extracted from the invoice.
     *
     * @param dueDate the due date. Can be null.
     */
    public void setDueDate(final LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Sets the bank name extracted from the invoice.
     *
     * @param bankName the bank name. Can be null.
     */
    public void setBankName(final String bankName) {
        this.bankName = bankName;
    }

    /**
     * Sets the last four digits of the card.
     *
     * @param cardLastFourDigits the last four digits. Can be null.
     */
    public void setCardLastFourDigits(final String cardLastFourDigits) {
        this.cardLastFourDigits = cardLastFourDigits;
    }

    // Getters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public CreditCard getCreditCard() { return creditCard; }
    public ImportStatus getStatus() { return status; }
    public ImportSource getSource() { return source; }
    public String getOriginalFileName() { return originalFileName; }
    public String getFilePath() { return filePath; }
    public String getParsedData() { return parsedData; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public Invoice getCreatedInvoice() { return createdInvoice; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public LocalDateTime getDueDate() { return dueDate; }
    public String getBankName() { return bankName; }
    public String getCardLastFourDigits() { return cardLastFourDigits; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceImport that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 