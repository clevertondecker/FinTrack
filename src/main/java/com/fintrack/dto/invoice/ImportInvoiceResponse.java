package com.fintrack.dto.invoice;

import com.fintrack.domain.invoice.ImportStatus;
import com.fintrack.domain.invoice.ImportSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for invoice import response.
 */
public record ImportInvoiceResponse(
    String message,
    Long importId,
    ImportStatus status,
    ImportSource source,
    String originalFileName,
    String errorMessage,
    LocalDateTime importedAt,
    LocalDateTime processedAt,
    BigDecimal totalAmount,
    String bankName,
    String cardLastFourDigits
) {
    /**
     * Creates an error response with only the error message populated.
     */
    public static ImportInvoiceResponse error(String errorMessage) {
        return new ImportInvoiceResponse(
            errorMessage, null, null, null, null,
            errorMessage, null, null, null, null, null
        );
    }
}