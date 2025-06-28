package com.fintrack.dto.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * DTO representing parsed invoice data extracted from files.
 */
public record ParsedInvoiceData(
    
    @JsonProperty("creditCardName")
    String creditCardName,
    
    @JsonProperty("cardNumber")
    String cardNumber, // last 4 digits
    
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
    Double confidence // 0.0 to 1.0, indicating parsing confidence
) {
    
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
        String category, // attempt at categorization
        
        @JsonProperty("installments")
        Integer installments,
        
        @JsonProperty("totalInstallments")
        Integer totalInstallments,
        
        @JsonProperty("confidence")
        Double confidence // 0.0 to 1.0, indicating item parsing confidence
    ) {}
} 