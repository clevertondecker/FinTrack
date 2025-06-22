package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for invoice item list.
 */
public record InvoiceItemListResponse(
    String message,
    Long invoiceId,
    List<InvoiceItemResponse> items,
    int count,
    BigDecimal totalAmount
) {} 