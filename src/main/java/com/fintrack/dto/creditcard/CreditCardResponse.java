package com.fintrack.dto.creditcard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditCardResponse(
    Long id,
    String name,
    String lastFourDigits,
    BigDecimal limit,
    boolean active,
    String bankName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {} 