package com.fintrack.dto.creditcard;

import java.math.BigDecimal;

public record CreditCardResponse(
    Long id,
    String name,
    String lastFourDigits,
    BigDecimal limit,
    boolean active,
    String bankName
) {} 