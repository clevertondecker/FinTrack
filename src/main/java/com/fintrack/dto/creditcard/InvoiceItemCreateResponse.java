package com.fintrack.dto.creditcard;

import java.math.BigDecimal;

/**
 * Response DTO for invoice item creation.
 */
public record InvoiceItemCreateResponse(
    String message,
    Long id,
    Long invoiceId,
    String description,
    BigDecimal amount,
    String category,
    BigDecimal invoiceTotalAmount
) {} 