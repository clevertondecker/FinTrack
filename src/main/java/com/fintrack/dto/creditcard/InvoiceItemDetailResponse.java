package com.fintrack.dto.creditcard;

/**
 * Response DTO for invoice item details.
 */
public record InvoiceItemDetailResponse(
    String message,
    InvoiceItemResponse item
) {} 