package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO for creating a new invoice item.
 * Contains validation annotations for request validation.
 */
public record CreateInvoiceItemRequest(
    
    @NotBlank(message = "Item description is required.")
    @Size(min = 2, max = 200, message = "Item description must be between 2 and 200 characters.")
    String description,
    
    @NotNull(message = "Item amount is required.")
    @Positive(message = "Item amount must be positive.")
    BigDecimal amount
) {} 