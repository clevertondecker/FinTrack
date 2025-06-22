package com.fintrack.dto.creditcard;

import java.util.List;

/**
 * Response DTO for invoices by credit card.
 */
public record InvoiceByCreditCardResponse(
    String message,
    Long creditCardId,
    String creditCardName,
    List<InvoiceResponse> invoices,
    int count
) {} 