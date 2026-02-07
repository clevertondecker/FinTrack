package com.fintrack.dto.creditcard;

import com.fintrack.domain.creditcard.CardType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditCardResponse(
    Long id,
    String name,
    String lastFourDigits,
    BigDecimal limit,
    boolean active,
    String bankName,
    CardType cardType,
    Long parentCardId,
    String parentCardName,
    String cardholderName,
    Long assignedUserId,
    String assignedUserName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {} 