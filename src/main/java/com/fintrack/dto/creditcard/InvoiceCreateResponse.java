package com.fintrack.dto.creditcard;

import java.time.LocalDate;

/**
 * Response DTO for invoice creation.
 */
public record InvoiceCreateResponse(
    String message,
    Long id,
    Long creditCardId,
    LocalDate dueDate,
    String status
) {} 