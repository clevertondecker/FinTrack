package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for invoice data.
 */
public record InvoiceResponse(
    Long id,
    Long creditCardId,
    String creditCardName,
    LocalDate dueDate,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    BigDecimal userShare
) {} 