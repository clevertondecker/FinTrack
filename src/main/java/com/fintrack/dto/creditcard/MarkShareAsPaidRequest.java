package com.fintrack.dto.creditcard;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * DTO for marking a share as paid.
 */
public record MarkShareAsPaidRequest(
    
    @NotNull(message = "Payment method is required.")
    @NotBlank(message = "Payment method cannot be blank.")
    String paymentMethod,
    
    @NotNull(message = "Payment date is required.")
    LocalDateTime paidAt
) {} 