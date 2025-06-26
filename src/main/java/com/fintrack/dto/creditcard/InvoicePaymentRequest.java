package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for invoice payment requests.
 * Contains the amount to be paid for an invoice.
 */
public record InvoicePaymentRequest(
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    BigDecimal amount
) {} 