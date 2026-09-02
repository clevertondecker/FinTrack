package com.fintrack.dto.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing parsed invoice data extracted from files.
 */
public record ParsedInvoiceData(
    
    @JsonProperty("creditCardName")
    String creditCardName,
    
    @JsonProperty("cardNumber")
    String cardNumber,
    
    @JsonProperty("dueDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate dueDate,
    
    @JsonProperty("totalAmount")
    BigDecimal totalAmount,
    
    @JsonProperty("items")
    List<ParsedInvoiceItem> items,
    
    @JsonProperty("bankName")
    String bankName,
    
    @JsonProperty("invoiceMonth")
    @JsonFormat(pattern = "yyyy-MM")
    YearMonth invoiceMonth,
    
    @JsonProperty("confidence")
    Double confidence,

    @JsonProperty("cardSections")
    List<ParsedCardSection> cardSections,

    @JsonProperty("reconciliation")
    ImportReconciliation reconciliation
) {

    /**
     * Backward-compatible constructor without cardSections.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ParsedInvoiceData(
            String creditCardName, String cardNumber, LocalDate dueDate,
            BigDecimal totalAmount, List<ParsedInvoiceItem> items,
            String bankName, YearMonth invoiceMonth, Double confidence) {
        this(creditCardName, cardNumber, dueDate, totalAmount, items,
             bankName, invoiceMonth, confidence, List.of(), ImportReconciliation.notApplicable());
    }

    /** Backward-compatible constructor for callers that provide card sections. */
    public ParsedInvoiceData(
            String creditCardName, String cardNumber, LocalDate dueDate,
            BigDecimal totalAmount, List<ParsedInvoiceItem> items,
            String bankName, YearMonth invoiceMonth, Double confidence,
            List<ParsedCardSection> cardSections) {
        this(creditCardName, cardNumber, dueDate, totalAmount, items,
             bankName, invoiceMonth, confidence, cardSections, ImportReconciliation.notApplicable());
    }

    /**
     * Returns all items across all card sections as a flat list.
     * Falls back to the direct items list when no sections exist.
     */
    public List<ParsedInvoiceItem> allItems() {
        if (cardSections != null && !cardSections.isEmpty()) {
            List<ParsedInvoiceItem> all = new ArrayList<>();
            for (ParsedCardSection section : cardSections) {
                if (section.items() != null) {
                    all.addAll(section.items());
                }
            }
            return all;
        }
        return items != null ? items : List.of();
    }

    /**
     * DTO for individual invoice items extracted from the invoice.
     */
    public record ParsedInvoiceItem(
        
        @JsonProperty("description")
        String description,
        
        @JsonProperty("amount")
        BigDecimal amount,
        
        @JsonProperty("purchaseDate")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate purchaseDate,
        
        @JsonProperty("category")
        String category,
        
        @JsonProperty("installments")
        Integer installments,
        
        @JsonProperty("totalInstallments")
        Integer totalInstallments,
        
        @JsonProperty("confidence")
        Double confidence
    ) {}

    /**
     * Represents a group of items belonging to a specific credit card detected in the PDF.
     */
    public record ParsedCardSection(

        @JsonProperty("cardLastFourDigits")
        String cardLastFourDigits,

        @JsonProperty("cardDisplayName")
        String cardDisplayName,

        @JsonProperty("items")
        List<ParsedInvoiceItem> items,

        @JsonProperty("subtotal")
        BigDecimal subtotal,

        @JsonProperty("declaredTotal")
        BigDecimal declaredTotal
    ) {
        public ParsedCardSection(
                String cardLastFourDigits, String cardDisplayName,
                List<ParsedInvoiceItem> items, BigDecimal subtotal) {
            this(cardLastFourDigits, cardDisplayName, items, subtotal, null);
        }
    }

    /**
     * Result of validating the totals declared in a bank statement against
     * the transactions extracted for each card section.
     */
    public record ImportReconciliation(
        @JsonProperty("status") ReconciliationStatus status,
        @JsonProperty("message") String message,
        @JsonProperty("difference") BigDecimal difference
    ) {
        public static ImportReconciliation reconciled() {
            return new ImportReconciliation(ReconciliationStatus.RECONCILED,
                "Statement totals reconciled.", BigDecimal.ZERO);
        }

        public static ImportReconciliation divergent(final BigDecimal difference) {
            return new ImportReconciliation(ReconciliationStatus.DIVERGENT,
                "One or more card totals do not match the statement.", difference);
        }

        public static ImportReconciliation reviewRequired() {
            return new ImportReconciliation(ReconciliationStatus.REVIEW_REQUIRED,
                "The statement does not contain enough totals to validate the import.", null);
        }

        public static ImportReconciliation notApplicable() {
            return new ImportReconciliation(ReconciliationStatus.NOT_APPLICABLE,
                "Reconciliation is not available for this statement format.", null);
        }

        public boolean blocksConfirmation() {
            return status == ReconciliationStatus.DIVERGENT
                || status == ReconciliationStatus.REVIEW_REQUIRED;
        }
    }

    public enum ReconciliationStatus {
        /** Parsed values match the totals printed by the statement. */
        RECONCILED,
        /** Parsed values conflict with the totals printed by the statement. */
        DIVERGENT,
        /** The parser cannot prove that this statement is safe to import. */
        REVIEW_REQUIRED,
        /** This bank format does not provide card-level reconciliation totals. */
        NOT_APPLICABLE
    }
}
