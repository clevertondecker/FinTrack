package com.fintrack.dto.creditcard;

import com.fintrack.domain.creditcard.CardType;
import java.math.BigDecimal;

public record CreditCardCreateResponse(
    String message,
    Long id,
    String name,
    String lastFourDigits,
    BigDecimal limit,
    String bankName,
    CardType cardType,
    String cardholderName
) {} 