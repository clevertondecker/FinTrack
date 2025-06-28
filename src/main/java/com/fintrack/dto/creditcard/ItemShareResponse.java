package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for item share data.
 */
public record ItemShareResponse(
    Long id,
    Long userId,
    String userName,
    String userEmail,
    BigDecimal percentage,
    BigDecimal amount,
    Boolean responsible,
    Boolean paid,
    String paymentMethod,
    LocalDateTime paidAt,
    LocalDateTime createdAt
) {} 