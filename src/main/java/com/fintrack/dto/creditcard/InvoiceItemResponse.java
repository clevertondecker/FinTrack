package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for invoice item data.
 */
public record InvoiceItemResponse(
    Long id,
    Long invoiceId,
    String description,
    BigDecimal amount,
    String category,
    String purchaseDate,
    LocalDateTime createdAt,
    Integer installments,
    Integer totalInstallments
) {} 