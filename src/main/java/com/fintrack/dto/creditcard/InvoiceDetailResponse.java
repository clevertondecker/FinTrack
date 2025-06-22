package com.fintrack.dto.creditcard;

/**
 * Response DTO for invoice details.
 */
public record InvoiceDetailResponse(
    String message,
    InvoiceResponse invoice
) {} 