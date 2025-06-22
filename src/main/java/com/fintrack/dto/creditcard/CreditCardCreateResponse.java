package com.fintrack.dto.creditcard;

import java.math.BigDecimal;

public record CreditCardCreateResponse(
    String message,
    Long id,
    String name,
    String lastFourDigits,
    BigDecimal limit,
    String bankName
) {} 