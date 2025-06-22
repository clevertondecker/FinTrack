package com.fintrack.dto.creditcard;

import java.util.List;

/**
 * Response DTO for invoice list.
 */
public record InvoiceListResponse(
    String message,
    List<InvoiceResponse> invoices,
    int count
) {} 