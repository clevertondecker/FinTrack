package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for bulk marking item shares as paid.
 */
public record MarkSharesAsPaidBulkRequest(
    @NotEmpty(message = "Share IDs are required.") List<Long> shareIds,
    @NotBlank(message = "Payment method is required.") String paymentMethod,
    @NotNull(message = "Payment date is required.") LocalDateTime paidAt
) {}
