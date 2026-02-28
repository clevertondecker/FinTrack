package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for invoice data.
 * Includes the authenticated user's share and a per-person breakdown
 * of other participants' shares (trusted contacts and other system users).
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
    BigDecimal userShare,
    List<ContactShareSummary> contactShares
) {}
