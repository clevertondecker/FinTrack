package com.fintrack.dto.invoice;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for importing an invoice from a file.
 */
public record ImportInvoiceRequest(
    
    @NotNull(message = "Credit card ID is required.")
    @Positive(message = "Credit card ID must be positive.")
    Long creditCardId
) {} 