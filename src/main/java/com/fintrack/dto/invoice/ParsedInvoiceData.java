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
    List<ParsedCardSection> cardSections
) {

    /**
     * Backward-compatible constructor without cardSections.
     */
    public ParsedInvoiceData(
            String creditCardName, String cardNumber, LocalDate dueDate,
            BigDecimal totalAmount, List<ParsedInvoiceItem> items,
            String bankName, YearMonth invoiceMonth, Double confidence) {
        this(creditCardName, cardNumber, dueDate, totalAmount, items,
             bankName, invoiceMonth, confidence, List.of());
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
        BigDecimal subtotal
    ) {}
} 