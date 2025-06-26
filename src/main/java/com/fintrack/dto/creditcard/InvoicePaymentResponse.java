package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for invoice payment responses.
 * Contains updated invoice information after payment processing.
 */
public record InvoicePaymentResponse(
    Long invoiceId,
    Long creditCardId,
    String creditCardName,
    LocalDate dueDate,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    String status,
    LocalDateTime updatedAt,
    String message
) {} 