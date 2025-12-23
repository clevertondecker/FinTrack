package com.fintrack.dto.invoice;

import com.fintrack.domain.invoice.ImportStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for tracking import progress and providing detailed status.
 */
public record ImportProgressResponse(
    Long importId,
    ImportStatus status,
    String message,
    LocalDateTime importedAt,
    LocalDateTime processedAt,
    String errorMessage,
    ParsedInvoiceData parsedData,
    BigDecimal totalAmount,
    String bankName,
    String cardLastFourDigits,
    Boolean requiresManualReview
) {} 