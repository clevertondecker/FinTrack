package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * DTO for creating a new invoice.
 * Contains validation annotations for request validation.
 */
public record CreateInvoiceRequest(
    
    @NotNull(message = "Credit card ID is required.")
    @Positive(message = "Credit card ID must be positive.")
    Long creditCardId,
    
    @NotNull(message = "Due date is required.")
    LocalDate dueDate
) {} 